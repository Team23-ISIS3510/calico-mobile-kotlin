# Google Login Frontend Checklist ✅

## Estado Actual (CORREGIDO)

### 1. ✅ Enviar token correcto (id_token)
**Archivo:** `app/src/main/java/com/calico/tutor/data/datasource/remote/GoogleSignInManager.kt`
```kotlin
suspend fun silentSignIn(): String? {
    return try {
        val account = googleSignInClient.silentSignIn().await()
        account?.idToken  // ✅ Envía id_token, NO access_token
    } catch (e: Exception) {
        null
    }
}
```
**Status:** ✅ Correcto

---

### 2. ✅ Usar el mismo CLIENT_ID
**Archivo:** `app/src/main/java/com/calico/tutor/ui/screen/AuthScreen.kt` (línea 37)
```kotlin
val googleWebClientId = context.getString(R.string.google_web_client_id)
GoogleSignInManager(activity, googleWebClientId)
```
**Configuración:** `app/build.gradle.kts`
```gradle
val googleWebClientId = localProperties.getProperty("GOOGLE_WEB_CLIENT_ID")
resValue("string", "google_web_client_id", googleWebClientId)
```
**Status:** ✅ Correcto - Usa `GOOGLE_WEB_CLIENT_ID` desde local.properties

---

### 3. ✅ No modificar el token
**Archivo:** `app/src/main/java/com/calico/tutor/data/repository/AuthRepositoryImpl.kt` (línea ~78)

**ANTES (❌ INCORRECTO):**
```kotlin
val firebaseIdToken = exchangeGoogleForFirebaseIdToken(idToken)  // ❌ TRANSFORMABA
val request = GoogleLoginRequest(idToken = firebaseIdToken, userId = null)
```

**DESPUÉS (✅ CORRECTO):**
```kotlin
// ✅ IMPORTANTE: Enviar el idToken de Google SIN transformar
// El backend es responsable de validar con Google
val request = GoogleLoginRequest(idToken = idToken, userId = null)
```
**Status:** ✅ CORREGIDO - Envía el token sin modificar

---

### 4. ✅ No validar el login en el frontend
**Archivo:** `app/src/main/java/com/calico/tutor/ui/viewmodel/AuthViewModel.kt`
```kotlin
fun loginWithGoogle(idToken: String, email: String? = null) {
    viewModelScope.launch {
        _authState.value = AuthState.Loading
        // Solo envía al backend, NO valida localmente
        val result = googleLoginUseCase(idToken, email)
        // Espera respuesta del backend
    }
}
```
**Status:** ✅ Correcto - Solo envía, no valida

---

## Flujo de Autenticación

```
1. Usuario → Toca "Sign in with Google"
   ↓
2. GoogleSignInManager.getSignInIntent()
   └─ Abre Google Sign-In dialog
   ↓
3. Usuario autentica con Google
   ↓
4. GoogleSignInManager recibe GoogleSignInAccount
   ├─ account.idToken ← ✅ CORRECTO
   └─ account.email
   ↓
5. AuthViewModel.loginWithGoogle(idToken, email)
   ↓
6. AuthRepositoryImpl.loginWithGoogle(idToken)
   ├─ Crea: GoogleLoginRequest(idToken = idToken)  ← ✅ SIN MODIFICAR
   ├─ POST → /auth/google-login
   └─ Recibe: AuthResponse(idToken, refreshToken, expiresIn)
   ↓
7. TokenManager.saveToken(...)
   └─ Guarda tokens del backend
   ↓
8. AuthState.Success → Navega a MainScreen
```

---

## Lo que el Backend debe hacer

> 📌 **IMPORTANTE:** El backend es responsable de validar el token de Google.

### ✅ Correcto:
```javascript
// Next.js backend - /auth/google-login endpoint
const { idToken } = req.body;

// 1. Validar con Google
const googleCertificates = await getGoogleCertificates();
const decoded = verify(idToken, googleCertificates);

// 2. Extraer información del usuario
const { email, name, picture } = decoded;

// 3. Buscar o crear usuario en BD
let user = await User.findOne({ email });
if (!user) {
    user = await User.create({ email, name, picture });
}

// 4. Generar tokens propios
const accessToken = generateToken(...);
const refreshToken = generateRefreshToken(...);

// 5. Responder con tokens propios
return {
    idToken: accessToken,      // ← Token PROPIO del backend
    refreshToken,              // ← Token PROPIO del backend
    expiresIn: 3600
};
```

### ❌ INCORRECTO:
```javascript
// ❌ NO hacer esto:
const token = await db.find({ googleToken: idToken });  // ❌ NO buscar en BD
const decoded = verify(idToken, "secret");              // ❌ NO verificar con secret
const verified = idToken === savedToken;                // ❌ NO comparar strings
```

---

## ✅ Checklist de Verificación

- [x] Frontend envía `idToken` de Google sin modificar
- [x] Frontend usa `GOOGLE_WEB_CLIENT_ID` correcto
- [x] Frontend no transforma el token
- [x] Frontend no valida localmente
- [ ] Backend recibe el `idToken` sin modificar (VERIFICAR)
- [ ] Backend valida el token con Google (VERIFICAR)
- [ ] Backend responde con tokens propios (VERIFICAR)
- [ ] Backend y frontend tienen el mismo `CLIENT_ID` (VERIFICAR)

---

## Debugging

Si sigue fallando:

1. **Verificar logs del frontend:**
   ```kotlin
   Log.d(TAG, "Enviando idToken al backend: ${idToken.take(50)}...")
   ```

2. **Verificar en backend:**
   ```javascript
   console.log("Recibido idToken:", idToken.substring(0, 50));
   ```

3. **Comparar CLIENT_ID:**
   - Android: `GOOGLE_WEB_CLIENT_ID` en `local.properties`
   - Backend: Variable de entorno o archivo de configuración
   - Ambos deben ser **exactamente iguales**

4. **Validar con Google:**
   - Ir a: https://www.googleapis.com/oauth2/v1/tokeninfo?id_token=TOKEN
   - Reemplazar TOKEN con el `idToken` del frontend
   - Verificar que Google lo reconoce

---

## Archivos Modificados

- ✅ `app/src/main/java/com/calico/tutor/data/repository/AuthRepositoryImpl.kt`
  - Removida transformación de Firebase
  - Removida función `exchangeGoogleForFirebaseIdToken()`
  - Enviado `idToken` sin modificar
