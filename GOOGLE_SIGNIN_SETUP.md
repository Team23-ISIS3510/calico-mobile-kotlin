# 🔐 Google Sign-In Setup Guide

## ¿Por qué no funciona el botón de Google Sign-In?
El código está completo pero **falta tu Web Client ID de Google Cloud Console**.

---

## 📋 Pasos para Configurar Google Sign-In

### PASO 1: Obtener Web Client ID en Google Cloud Console

1. Ve a https://console.cloud.google.com
2. **Crea un nuevo proyecto** o selecciona uno existente
3. Ve a **APIs y Servicios** → **OAuth consent screen**
   - Completa la información del consentimiento
4. Ve a **Credenciales** → **+ Crear credencial** → **OAuth 2.0 - ID de cliente**
   - Selecciona **Aplicación web**
   - URIs de redirección autorizados: 
     - `http://localhost`
     - Añade el SHA-1 de tu certificado de Androiddebug (obtén con `keytool -list -v -keystore ~/.android/debug.keystore`)
5. **Copia el Web Client ID** (formato: `1234567890-abcdefg.apps.googleusercontent.com`)

### PASO 2: Reemplazar el Placeholder en el Código

Abre el archivo: `app/src/main/java/com/calico/tutor/ui/screen/AuthScreen.kt`

Busca esta línea (aproximadamente línea 32):
```kotlin
val webClientId = "YOUR_WEB_CLIENT_ID.apps.googleusercontent.com"
```

Reemplázala con tu Web Client ID:
```kotlin
val webClientId = "1234567890-abcdefg.apps.googleusercontent.com"  // TU CLIENTE ID
```

### PASO 3: Verificar Dependencias

Asegúrate de que en `build.gradle.kts` esté:
```gradle
implementation(libs.firebase.auth)
implementation(libs.google.play.services.auth)
```

✅ Ya están instaladas.

### PASO 4: Verificar AndroidManifest.xml

Asegúrate de que tenga:
```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
```

✅ Ya están agregados.

### PASO 5: Prueba el Flujo Completo

1. Compila y ejecuta la app
2. Ve a la pantalla de Login
3. Haz clic en **"Sign in with Google"**
4. El flujo debería ser:
   - ✅ Se abre la ventana de Google
   - ✅ Inicia sesión con tu cuenta
   - ✅ Obtiene el idToken
   - ✅ Se autentica con Firebase
   - ✅ Se envía el token al backend (`POST /auth/google-login`)
   - ✅ Se guarda el token localmente
   - ✅ Se navega a HomeScreen

---

## 🔍 Si Algo Falla

El código ahora muestra **Toast messages** con el error exacto:

| Error | Causa | Solución |
|-------|-------|----------|
| "Google Play Services está desactualizado" | GooglePlay Services necesita actualización | Actualiza GooglePlay en el dispositivo |
| "El usuario canceló el inicio de sesión" | Usuario presionó atrás | Normal, el usuario rechazó |
| "No se obtuvo el ID Token" | Problema con Google Sign-In | Verifica que el Web Client ID sea correcto |
| "Error de API de Google" | Web Client ID inválido | Copia nuevamente desde Google Cloud |
| "Reemplaza 'YOUR_WEB_CLIENT_ID'" | Aún con el placeholder | **REEMPLÁZALO CON TU CLIENTE ID** |

---

## 📝 Flujo Técnico Completo

```
1. Usuario hace clic en "Sign in with Google"
   ↓
2. GoogleSignInManager.getSignInIntent() abre la pantalla de Google
   ↓
3. Usuario se autentica con Google
   ↓
4. AuthScreen recibe el idToken de Google
   ↓
5. Se llama a AuthViewModel.loginWithGoogle(idToken)
   ↓
6. GoogleLoginUseCase valida el idToken
   ↓
7. AuthRepository envía POST a /auth/google-login con:
   {
     "idToken": "...",
     "user_id": null
   }
   ↓
8. Backend retorna:
   {
     "idToken": "...jwt...",
     "refreshToken": "...",
     "expiresIn": 3600
   }
   ↓
9. Se guardan los tokens en TokenManager (SharedPreferences)
   ↓
10. AuthState cambia a Success
    ↓
11. Se navega a HomeScreen
```

---

## 🔒 Headers para Futuras Solicitudes

Después de autenticarse, **TODOS** los headers de API deben incluir:

```
Authorization: Bearer {idToken}
```

Esto ya está implementado en `RetrofitClient` con el `TokenManager`.

---

## ✅ Checklist Final

- [ ] Web Client ID obtenido de Google Cloud Console
- [ ] Reemplazado en AuthScreen.kt
- [ ] Permisos de internet en AndroidManifest.xml
- [ ] Dependencias Firebase y Google Play Services en build.gradle.kts
- [ ] Backend esperando POST en /auth/google-login
- [ ] Backend retornando AuthResponse con tokens
- [ ] TestProbando en el dispositivo/emulador

---

## 📞 Si Necesitas Ayuda

1. Revisa los Toast messages (errores en la pantalla)
2. Revisa los Logs con: `adb logcat | grep GoogleSignInManager`
3. Verifica que el Web Client ID sea exacto (sin espacios)
4. Asegúrate de que Firebase esté habilitado en tu proyecto de Google Cloud

---

**¿Listo? Obtén tu Web Client ID y reemplázalo en `AuthScreen.kt`. ¡Será todo!**
