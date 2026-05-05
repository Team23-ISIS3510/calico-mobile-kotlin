# Google Login Fix - Resumen de Cambios

## ⚠️ Problema Encontrado

El frontend estaba **transformando el token de Google** antes de enviarlo al backend, lo cual violaría el principio de "no modificar el token".

```
Google API
   ↓
idToken de Google
   ↓
❌ exchangeGoogleForFirebaseIdToken()  ← PROBLEMA
   ↓
idToken de Firebase (modificado)
   ↓
Enviado al backend ← INCORRECTO
```

### ¿Por qué es un problema?

- El backend espera el `id_token` de Google sin modificar
- El backend valida directamente con Google
- Transformar el token puede causar errores en la validación
- El token transformado podría no ser reconocido por Google

---

## ✅ Solución Aplicada

### Cambio Principal

**Archivo:** `AuthRepositoryImpl.kt`

```diff
- override suspend fun loginWithGoogle(idToken: String, email: String?): Result<AuthToken> {
+ override suspend fun loginWithGoogle(idToken: String, email: String?): Result<AuthToken> {
      return try {
-         Log.d(TAG, "Enviando idToken de Google al backend...")
+         Log.d(TAG, "Enviando idToken de Google sin modificar al backend...")
-         val firebaseIdToken = exchangeGoogleForFirebaseIdToken(idToken)
- 
-         val request = GoogleLoginRequest(idToken = firebaseIdToken, userId = null)
+         // ✅ IMPORTANTE: Enviar el idToken de Google SIN transformar
+         // El backend es responsable de validar con Google
+         val request = GoogleLoginRequest(idToken = idToken, userId = null)
```

### Después del Fix

```
Google API
   ↓
idToken de Google
   ↓
✅ Enviado directamente al backend (sin transformar)
   ↓
Backend valida con Google
```

---

## 📝 Funciones Removidas

1. **`exchangeGoogleForFirebaseIdToken(googleIdToken: String)`**
   - Llamaba a: `https://identitytoolkit.googleapis.com/v1/accounts:signInWithIdp`
   - Transformaba el token de Google en token de Firebase
   - **YA NO NECESARIA** - El backend es responsable de validación

### Imports Removidos

```kotlin
// ❌ Ya no se usan:
import com.calico.tutor.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.net.URLEncoder
```

---

## 🔄 Flujo de Autenticación (Después del Fix)

```
┌─────────────────────────────────────┐
│  Usuario → Click "Sign with Google" │
└────────────────┬────────────────────┘
                 ↓
         ┌───────────────┐
         │  Google SDK   │
         │  Sign-In      │
         └───────┬───────┘
                 ↓
         ┌──────────────────┐
         │ GoogleSignAccount │ ← Account con idToken
         │ - idToken ✅      │
         │ - email          │
         └───────┬──────────┘
                 ↓
      ┌──────────────────────┐
      │ AuthViewModel        │
      │ loginWithGoogle()    │
      └──────────┬───────────┘
                 ↓
      ┌──────────────────────────────┐
      │ AuthRepositoryImpl            │
      │ loginWithGoogle()             │
      ├──────────────────────────────┤
      │ GoogleLoginRequest {          │
      │   idToken: "JWT_DE_GOOGLE"✅  │
      │   (sin modificar)             │
      │ }                             │
      └──────────┬───────────────────┘
                 ↓
      ┌──────────────────────┐
      │ POST /auth/google    │
      │ -login               │
      │ Body: {idToken: ...} │
      └──────────┬───────────┘
                 ↓
      ┌──────────────────────────┐
      │ Backend (Next.js)        │
      ├──────────────────────────┤
      │ 1. Recibe idToken        │
      │ 2. Valida con Google     │
      │ 3. Busca usuario en BD   │
      │ 4. Genera tokens propios │
      │ 5. Responde con tokens  │
      └──────────┬───────────────┘
                 ↓
      ┌──────────────────────────┐
      │ AuthResponse             │
      │ - idToken (del backend)✅│
      │ - refreshToken           │
      │ - expiresIn              │
      └──────────┬───────────────┘
                 ↓
      ┌──────────────────────────┐
      │ TokenManager             │
      │ Guarda tokens del backend│
      └──────────┬───────────────┘
                 ↓
      ┌──────────────────────────┐
      │ AuthState.Success        │
      │ Navega a MainScreen      │
      └──────────────────────────┘
```

---

## ✅ Verificación del Estado Actual

### Frontend Kotlin (Verificado ✅)

| Requerimiento | Archivo | Línea | Status |
|--------------|---------|-------|--------|
| Envía `idToken` | `GoogleSignInManager.kt` | 34 | ✅ Correcto |
| Sin transformar | `AuthRepositoryImpl.kt` | 80 | ✅ FIXED |
| Usa `GOOGLE_WEB_CLIENT_ID` | `AuthScreen.kt` | 37 | ✅ Correcto |
| No valida localmente | `AuthViewModel.kt` | 120 | ✅ Correcto |

### Backend Next.js (Verificar 👀)

| Requerimiento | Status | Notas |
|--------------|--------|-------|
| Recibe `idToken` sin modificar | 👀 Verificar | Ver logs del backend |
| Valida con Google | 👀 Verificar | Debe llamar a Google API |
| Responde con tokens propios | 👀 Verificar | NO debe responder con Google token |
| Usa mismo `CLIENT_ID` | 👀 Verificar | Debe coincidir exactamente |

---

## 📋 Checklist de Siguientes Pasos

### Para Backend Developer:

- [ ] Verificar que el endpoint `/auth/google-login` recibe el `id_token` de Google
- [ ] Validar el token directamente con Google (no con Firebase)
- [ ] Comparar que el `CLIENT_ID` usado en frontend coincide exactamente
- [ ] Responder con tokens propios del backend (no del Google)
- [ ] Guardar usuario en base de datos si no existe
- [ ] Revisar logs del backend para debugging

### Para Frontend Developer:

- [ ] Compilar el proyecto para aplicar los cambios
- [ ] Probar el login con Google completo
- [ ] Verificar en Logcat que el token se envía sin modificar
- [ ] Confirmar que el `GOOGLE_WEB_CLIENT_ID` es el correcto

---

## 🐛 Debugging si aún falla

### 1. Verificar token en frontend
```kotlin
Log.d("GoogleLogin", "idToken recibido de Google: ${idToken.take(50)}...")
Log.d("GoogleLogin", "idToken enviado al backend: ${idToken.take(50)}...")
// Deben ser IDÉNTICOS
```

### 2. Verificar en backend
```javascript
console.log("idToken recibido:", req.body.idToken.substring(0, 50));
```

### 3. Validar token con Google
```
GET https://www.googleapis.com/oauth2/v1/tokeninfo?id_token=YOUR_TOKEN
```
Debe responder con el usuario válido.

### 4. Verificar CLIENT_ID
```javascript
// Frontend: app/build.gradle.kts
val googleWebClientId = localProperties.getProperty("GOOGLE_WEB_CLIENT_ID")

// Backend: .env o config
GOOGLE_CLIENT_ID=...

// Ambos deben ser EXACTAMENTE IGUALES
```

---

## 📚 Referencias

- [Google Sign-In for Android](https://developers.google.com/identity/sign-in/android)
- [ID Token Validation](https://developers.google.com/identity/sign-in/web/backend-auth)
- [JWT Token Info Endpoint](https://www.googleapis.com/oauth2/v1/tokeninfo)

---

**Última actualización:** 2026-04-28
**Estado:** ✅ Frontend FIXED - Pendiente verificación en Backend
