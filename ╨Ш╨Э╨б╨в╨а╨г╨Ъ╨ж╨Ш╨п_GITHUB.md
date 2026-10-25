# Как собрать APK через GitHub Actions

## Шаг 1. Структура репозитория

В корне репозитория `schedulegrock` должны лежать:

```
schedulegrock/
├── .github/
│   └── workflows/
│       └── build-apk.yml
├── app/
│   ├── build.gradle.kts
│   └── src/
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
└── gradle/
    └── wrapper/
        └── gradle-wrapper.properties
```

Если ты просто залил zip — убедись, что **нет лишней вложенной папки**.

## Шаг 2. Добавь workflow

1. В репозитории нажми **Add file → Create new file**
2. Имя файла: `.github/workflows/build-apk.yml`
3. Вставь содержимое файла `build-apk.yml` (он уже есть в проекте)
4. Нажми **Commit changes**

## Шаг 3. Запусти сборку

1. Перейди во вкладку **Actions**
2. Слева выбери **Build APK**
3. Справа нажми **Run workflow** → **Run workflow**
4. Подожди 3–6 минут
5. Когда статус станет зелёным — открой сборку
6. Внизу в разделе **Artifacts** скачай **ScheduleWidget-APK**
7. Внутри будет `app-debug.apk`

## Если сборка упала (красным)

Открой failed job → смотри лог красной строки.

Частые ошибки:
- `SDK location not found` → уже исправлено в workflow
- `Could not find com.android.tools.build:gradle` → проверь `build.gradle.kts`
- `Task assembleDebug not found` → неправильная структура папок (app/ должна быть в корне)

---

## Альтернатива — собрать на компьютере

1. Установи Android Studio
2. File → Open → папка ScheduleWidgetApp
3. Build → Build APK(s)
4. APK будет в `app/build/outputs/apk/debug/`
