# 表情符號電池（Emoji Battery）

個人用 Android 應用：在狀態列附近以 `SYSTEM_ALERT_WINDOW` 覆蓋層顯示寶可夢貼圖 + 即時電量百分比。

## 環境路徑（本機 box）

| 項目 | 路徑 |
|------|------|
| 專案 | `/workspace/emoji-battery/` |
| JDK 17 | `/workspace/jdk/jdk-17.0.20.1+1` |
| Android SDK | `/workspace/android-sdk` |
| 產出 APK | `/workspace/emoji-battery/artifacts/EmojiBattery.apk` |

## 安裝 APK（側載）

1. 將 `artifacts/EmojiBattery.apk` 傳到手機會話（adb / 檔案傳輸）。
2. 在手機上允許「安裝未知應用程式」。
3. 安裝並開啟「Emoji Battery / 表情符號電池」。

```bash
adb install -r /workspace/emoji-battery/artifacts/EmojiBattery.apk
```

## 授權「顯示在其他應用程式上層」

1. 開啟 App → 點「前往授權 / Open permission settings」。
2. 系統設定中開啟本 App 的「Display over other apps」。
3. 回到 App，確認狀態顯示「已授權 ✓」。

部分品牌路徑名稱不同（小米：顯示懸浮窗；Samsung：Appears on top）。

## 啟用小工具

1. 授權完成後，開啟「啟用電池小工具」開關。
2. 選擇貼圖（預設 **Treecko / 木守宮**）、大小、水平位置。
3. 狀態列附近會出現貼圖坐在電池膠囊上、中央顯示如 `67%`。
4. 執行期間會有前景服務通知（可點回設定頁）。

## 內建貼圖

來自 [PokeAPI/sprites](https://github.com/PokeAPI/sprites) 的 official artwork：

- Treecko (252)、Pikachu (25)、Eevee (133)、Mudkip (258)、Torchic (255)、Squirtle (7)

**版權聲明：** Pokémon 與精靈圖 © Nintendo / Creatures Inc. / GAME FREAK。素材經 PokeAPI/sprites 取得，僅供個人使用，請勿重新散布作商業用途。

## 重新編譯

```bash
export JAVA_HOME=/workspace/jdk/jdk-17.0.20.1+1
export ANDROID_HOME=/workspace/android-sdk
export PATH="$JAVA_HOME/bin:$PATH"

cd /workspace/emoji-battery
./gradlew :app:assembleDebug

# 複製正式交付檔
mkdir -p artifacts
cp app/build/outputs/apk/debug/app-debug.apk artifacts/EmojiBattery.apk
```

`local.properties` 已指向 `sdk.dir=/workspace/android-sdk`。

## 注意事項 / Caveats

- Overlay 使用 `TYPE_APPLICATION_OVERLAY`，**不是** Accessibility Service。
- 無法真正替換系統狀態列電池圖示；是疊在狀態列區域附近的小工具。
- OEM（小米、OPPO、vivo、華為等）可能殺背景服務：請關閉電池最佳化、允許自啟動。
- 瀏海 / 打孔螢幕上位置可能需手動調「靠左/置中/靠右」。
- Debug APK，僅供個人側載。
