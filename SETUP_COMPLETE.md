# 📱 Calico Mobile Kotlin - Configuración Completada

## ✅ Estado Actual
El proyecto está **totalmente configurado y corriendo** en el emulador Android.

### Lo que se hizo:
1. ✅ Compilación exitosa del proyecto (usando JDK 22)
2. ✅ APK generado: `app/build/outputs/apk/debug/app-debug.apk`
3. ✅ Emulador iniciado: `Medium_Phone_API_36.1`
4. ✅ App instalada en el emulador
5. ✅ App lanzada y corriendo

---

## 🚀 Cómo Correr el Proyecto Ahora

### Opción 1: Desde VS Code (Recomendado)
1. Abre la pestaña **Ejecutar y Depurar** (Ctrl + Shift + D)
2. Selecciona una configuración:
   - **"Android: Build & Run Debug"** - Compila e instala la app automáticamente
   - **"Android: View Logcat"** - Ve los logs en tiempo real
   - **"Android: Start Emulator"** - Inicia el emulador (si lo cerraste)

### Opción 2: Desde Terminal
```powershell
# Compilar e instalar
$env:JAVA_HOME="C:\Program Files\Java\jdk-22"
.\gradlew.bat installDebug

# Lanzar la app
& "C:\Users\asus\AppData\Local\Android\Sdk\platform-tools\adb.exe" shell am start -n com.calico.tutor/com.calico.tutor.MainActivity

# Ver logs en tiempo real
& "C:\Users\asus\AppData\Local\Android\Sdk\platform-tools\adb.exe" logcat -s "calico:*" -v threadtime
```

---

## 📋 Estructura del Proyecto

```
app/
├── build.gradle.kts          # Configuración de compilación
├── src/
│   └── main/
│       ├── java/com/calico/tutor/
│       │   ├── CalicoApp.kt           # Clase Application
│       │   ├── MainActivity.kt        # Actividad principal
│       │   ├── ui/                   # Interfaz de usuario (Jetpack Compose)
│       │   │   ├── screen/           # Pantallas (LoginScreen, RegisterScreen)
│       │   │   ├── viewmodel/        # ViewModels (AuthViewModel)
│       │   │   ├── theme/            # Temas y estilos
│       │   │   └── component/        # Componentes reutilizables
│       │   ├── domain/               # Lógica de negocio (Clean Architecture)
│       │   │   ├── model/            # Modelos puros
│       │   │   ├── repository/       # Interfaces de repositorio
│       │   │   └── usecase/          # Casos de uso
│       │   ├── data/                 # Capa de datos
│       │   │   ├── datasource/       # API remota y almacenamiento local
│       │   │   ├── repository/       # Implementación de repositorios
│       │   │   ├── dto/              # Modelos DTOs
│       │   │   └── mapper/           # Mapeo de DTOs a modelos
│       │   └── di/                   # Inyección de dependencias
│       └── res/                      # Recursos (layouts, strings, etc.)
```

---

## 🛠️ Configuración del Entorno

- **Java**: JDK 22 (C:\Program Files\Java\jdk-22)
- **Gradle**: 9.1.0
- **Android SDK**: C:\Users\asus\AppData\Local\Android\Sdk
- **Emulador**: Medium_Phone_API_36.1 (Android 16)
- **Min SDK**: 24, **Target SDK**: 36

---

## 📦 Dependencias Principales

- **Jetpack Compose** - UI moderna y reactiva
- **Retrofit** - Cliente HTTP para API
- **Coroutines** - Programación asincrónica
- **ViewModel & StateFlow** - Gestión de estado
- **Security Crypto** - Almacenamiento seguro de tokens

---

## 📝 Próximos Pasos

1. **Editar código**: Los cambios se compilarán automáticamente
2. **Ver logs**: Abre la terminal de logcat para debugging
3. **Hot Reload**: Los cambios en UI (Compose) se ven casi en tiempo real

¡Tu app está lista para desarrollar! 🎉
