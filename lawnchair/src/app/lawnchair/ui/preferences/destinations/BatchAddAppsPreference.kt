/*
 * Copyright 2024, Lawnchair
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package app.lawnchair.ui.preferences.destinations

import android.widget.Toast
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.lawnchair.ui.OverflowMenu
import app.lawnchair.ui.preferences.LocalIsExpandedScreen
import app.lawnchair.ui.preferences.LocalNavController
import app.lawnchair.ui.preferences.components.AppItem
import app.lawnchair.ui.preferences.components.AppItemPlaceholder
import app.lawnchair.ui.preferences.components.layout.PreferenceDivider
import app.lawnchair.ui.preferences.components.layout.PreferenceLazyColumn
import app.lawnchair.ui.preferences.components.layout.PreferenceScaffold
import app.lawnchair.ui.preferences.components.layout.preferenceGroupItems
import app.lawnchair.util.App
import app.lawnchair.util.appsState
import com.android.launcher3.R
import com.android.launcher3.model.ItemInstallQueue

// インストール済みアプリを選択してホーム画面に一括追加する設定画面
@Composable
fun BatchAddAppsPreference(
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val navController = LocalNavController.current
    val apps by appsState()

    // 選択状態を管理（デフォルト全選択）
    var selectedApps by remember(apps) {
        mutableStateOf(apps.map { it.key.toString() }.toSet())
    }

    val pageTitle = if (selectedApps.isEmpty()) {
        stringResource(id = R.string.batch_add_apps_title)
    } else {
        stringResource(id = R.string.batch_add_apps_title) + " (${selectedApps.size})"
    }

    val state = rememberLazyListState()

    PreferenceScaffold(
        label = pageTitle,
        actions = {
            if (apps.isNotEmpty()) {
                // 全選択/全解除メニュー
                BatchAddSortingOptions(
                    originalList = apps,
                    selectedApps = selectedApps,
                    onUpdateSelection = { selectedApps = it },
                )
            }
        },
        bottomBar = {
            // ホーム画面に追加するボタン
            if (selectedApps.isNotEmpty()) {
                Button(
                    onClick = {
                        // 選択されたアプリをItemInstallQueueに積む
                        val queue = ItemInstallQueue.INSTANCE.get(context)
                        val selectedAppObjects = apps.filter {
                            selectedApps.contains(it.key.toString())
                        }
                        selectedAppObjects.forEach { app ->
                            queue.queueItem(app.key.componentName.packageName, app.key.user)
                        }

                        // 追加結果をToastで通知
                        Toast.makeText(
                            context,
                            context.getString(R.string.batch_add_apps_toast, selectedAppObjects.size),
                            Toast.LENGTH_SHORT,
                        ).show()

                        // 前の画面に戻る
                        navController.popBackStack()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                ) {
                    Text(text = stringResource(id = R.string.batch_add_apps_execute))
                }
            }
        },
        modifier = modifier,
        isExpandedScreen = LocalIsExpandedScreen.current,
    ) {
        Crossfade(targetState = apps.isNotEmpty(), label = "") { present ->
            if (present) {
                PreferenceLazyColumn(it, state = state) {
                    // アプリの選択/選択解除を切り替える
                    val toggleApp = { app: App ->
                        val key = app.key.toString()
                        selectedApps = if (selectedApps.contains(key)) {
                            selectedApps - key
                        } else {
                            selectedApps + key
                        }
                    }
                    preferenceGroupItems(
                        items = apps,
                        isFirstChild = true,
                        dividerStartIndent = 40.dp,
                    ) { _, app ->
                        AppItem(
                            app = app,
                            onClick = toggleApp,
                        ) {
                            Checkbox(
                                checked = selectedApps.contains(app.key.toString()),
                                onCheckedChange = null,
                            )
                        }
                    }
                }
            } else {
                // ローディング中のプレースホルダー表示
                PreferenceLazyColumn(it, enabled = false) {
                    preferenceGroupItems(
                        count = 20,
                        isFirstChild = true,
                        dividerStartIndent = 40.dp,
                    ) {
                        AppItemPlaceholder {
                            Spacer(Modifier.width(24.dp))
                        }
                    }
                }
            }
        }
    }
}

// 全選択/全解除/選択反転メニュー
@Composable
private fun BatchAddSortingOptions(
    originalList: List<App>,
    selectedApps: Set<String>,
    onUpdateSelection: (Set<String>) -> Unit,
    modifier: Modifier = Modifier,
) {
    OverflowMenu(modifier) {
        // 選択反転
        DropdownMenuItem(
            onClick = {
                val inverseSelection = originalList
                    .map { it.key.toString() }
                    .filter { !selectedApps.contains(it) }
                    .toSet()
                onUpdateSelection(inverseSelection)
                hideMenu()
            },
            text = {
                Text(stringResource(R.string.inverse_selection))
            },
        )
        // 全選択/全解除
        val allKeys = originalList.map { it.key.toString() }.toSet()
        val isAllSelected = allKeys == selectedApps
        DropdownMenuItem(
            onClick = {
                onUpdateSelection(
                    if (isAllSelected) {
                        emptySet()
                    } else {
                        allKeys
                    },
                )
                hideMenu()
            },
            text = {
                Text(
                    stringResource(if (isAllSelected) R.string.deselect_all else R.string.select_all),
                )
            },
        )
        PreferenceDivider(modifier = Modifier.padding(vertical = 8.dp))
        // リセット（全解除）
        DropdownMenuItem(
            onClick = {
                onUpdateSelection(emptySet())
                hideMenu()
            },
            text = {
                Text(stringResource(R.string.action_reset))
            },
        )
    }
}
