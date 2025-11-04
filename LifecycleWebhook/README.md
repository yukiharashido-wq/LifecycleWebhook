# LifecycleWebhook (Paper 1.19.2, Java 17)

Auto gửi Discord webhook embed:
- Server Online/Offline
- Plugin Enabled/Disabled
- Plugin Updated (phát hiện thay đổi version)

## Build
```bash
mvn -q -f pom.xml package
```
Jar nằm ở `target/lifecycle-webhook-1.0.0-shaded.jar`.

## Cài đặt
- Thả jar vào `plugins/`
- Start server 1 lần -> sinh `plugins/LifecycleWebhook/config.yml`
- Dán `webhook_url`
- Restart server
