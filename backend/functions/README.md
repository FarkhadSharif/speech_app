# Firebase Function: revokeOwnSessions

Функция `revokeOwnSessions` завершает Firebase Authentication-сессии текущего родителя на всех устройствах. Она принимает только Firebase Callable-запрос с действующим контекстом авторизации и берёт `uid` исключительно из `request.auth.uid`. Данные запроса, включая переданный клиентом `uid`, намеренно игнорируются.

## Локальная проверка

```powershell
cd backend/functions
npm ci
npm test
npm run lint
```

## Развёртывание

Развёртывание в production из этого репозитория намеренно не выполняется автоматически.

1. Установите Firebase CLI и войдите в аккаунт: `npx firebase-tools login`.
2. В корне проекта выберите тот же Firebase-проект, чей `google-services.json` используется Android-приложением: `npx firebase-tools use --add`.
3. Убедитесь, что для проекта разрешено развёртывание Cloud Functions и выбран подходящий тарифный план.
4. Запустите тесты из `backend/functions`.
5. Из корня проекта выполните: `npx firebase-tools deploy --only functions:revokeOwnSessions`.

После отзыва refresh tokens текущая сессия Android удаляется приложением сразу. На других устройствах уже выданный краткоживущий ID token может действовать до обновления; затем повторное получение токена будет отклонено Firebase.
