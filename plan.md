# アプリアイコンとドックのグリッドレイアウトで、アイコンの位置（座標）の計算方法の調査結果

## 概要
Android Launcher3 (Quickstep) では、ホーム画面（Workspace）とドック（Hotseat / Taskbar）でアイコンの位置（座標）の計算方法が異なります。ホーム画面は `CellLayout` というグリッドベースのレイアウトシステムを使用していますが、ドックはデバイスの状態（スマートフォンかタブレットか、ジェスチャーナビゲーションかなど）によって `Hotseat` または `TaskbarView` を使用します。

## 1. ホーム画面（Workspace）のアイコン位置計算
ホーム画面のアイコンは `CellLayout` というカスタムの `ViewGroup` 内に配置されます。各アイコンのレイアウト情報は `CellLayoutLayoutParams` によって管理されます。

### 計算の仕組み
1. **グリッド座標の保持**: 各アイコンは、グリッド上のどのセルに配置されるか（`mCellX`, `mCellY`）と、何セル分を占有するか（`cellHSpan`, `cellVSpan`）という情報を持っています。
2. **ピクセル座標への変換**: `CellLayoutLayoutParams` の `setup()` メソッドで、グリッド座標から実際のピクセル座標（`x`, `y`）とサイズ（`width`, `height`）が計算されます。
   * **セルサイズの計算**: デバイスプロファイル（`DeviceProfile`）に基づいて計算されたセルの幅（`cellWidth`）と高さ（`cellHeight`）を使用します。
   * **ボーダースペースの考慮**: セル間の隙間（`borderSpace`）も計算に含まれます。
   * **RTL（右体左）対応**: `invertHorizontally` が true の場合、X座標が反転されます。
   * **マージンの適用**: アイコンの上下左右のマージンが引かれます。

### `setup()` メソッドの計算式 (一部抜粋)
```java
// x, y 座標の計算
x = leftMargin + (myCellX * cellWidth) + (myCellX * borderSpace.x);
y = topMargin + (myCellY * cellHeight) + (myCellY * borderSpace.y);

// 幅、高さの計算
float myCellWidth = ((myCellHSpan * cellWidth) + hBorderSpacing) / cellScaleX;
float myCellHeight = ((myCellVSpan * cellHeight) + vBorderSpacing) / cellScaleY;
width = Math.round(myCellWidth) - leftMargin - rightMargin;
height = Math.round(myCellHeight) - topMargin - bottomMargin;
```

3. **配置と描画**: `ShortcutAndWidgetContainer` (CellLayout内の実際の子ビューを保持するコンテナ) の `layoutChild()` メソッドで、計算された `x`, `y`, `width`, `height` を使って `child.layout()` が呼ばれ、最終的な位置が決定されます。

## 2. ドック（Hotseat / Taskbar）のアイコン位置計算

ドックはデバイスのフォームファクタによってコンポーネントが異なります。

### スマートフォンの場合 (Hotseat)
スマートフォンでは、ドックは `Hotseat` というクラスで表現されます。これは `CellLayout` を継承しており、基本的な位置計算はホーム画面と同じようにグリッドシステム（`CellLayoutLayoutParams`）を利用します。

*   **グリッドサイズ**: `Hotseat` の `resetLayout()` メソッドで、`DeviceProfile` に定義された表示アイコン数 (`numShownHotseatIcons`) を基に、1行N列（またはN行1列）のグリッドサイズが設定されます。
*   **アイコンの配置**: `getCellXFromOrder()`, `getCellYFromOrder()` を使って、アイコンのインデックスからグリッドの X/Y 座標を決定し、その後のピクセル座標への変換はホーム画面の `CellLayout` と同じロジックが使用されます。

### タブレット/折りたたみ端末の場合 (TaskbarView)
大画面デバイスでは、ドックは画面下部に浮かぶ「タスクバー」として機能し、`TaskbarView` (FrameLayout) によって実装されています。ここではグリッドシステムを使用せず、独自のレイアウトロジックでアイコンを配置します。

#### 計算の仕組み (`TaskbarView.onLayout()`)
1. **タスクバー領域の決定**: `DeviceProfile` やインセット（ノッチなど）の情報を基に、タスクバー全体の領域 (`mIconLayoutBounds`) が決定されます。
2. **アイコンの配置**: 右から左へ（RTLの場合は逆）順番にアイコンを配置していきます。
   * **QSB（検索バー）の配置**: 検索バーがある場合は、まずその領域を確保します。
   * **アイコン間のマージン**: 各アイコンの間には `mItemMarginLeftRight` というマージンが設けられます。
   * **アイコンのレイアウト**: `iconEnd`（右端の座標）からアイコンのタッチサイズ（`mIconTouchSize`）を引いて `iconStart`（左端の座標）を計算し、`child.layout(iconStart, top, iconEnd, bottom)` を呼び出して配置します。
3. **中央揃え**: タスクバーが画面幅全体を占めない場合、配置されたアイコン全体が中央にくるように `mIconLayoutBounds` が調整されることがあります。

## まとめ
* **ホーム画面**: `CellLayout` によるグリッドベースの計算。`CellLayoutLayoutParams.setup()` でグリッド座標からピクセル座標へ変換される。
* **ドック (スマホ)**: `Hotseat` を使用し、内部的には `CellLayout` と同じグリッドベースの計算。1行（または1列）のグリッド。
* **ドック (タブレット)**: `TaskbarView` を使用し、右から左へ固定のマージンを取りながら順番に要素を並べるリニアなレイアウト計算（`onLayout` メソッドで独自計算）。
