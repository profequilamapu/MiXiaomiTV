# Mi Xiaomi TV — Launcher ligero

Launcher experimental para Android TV/Google TV con 7 accesos: YouTube, IPTV Stream Player, Zapping, Netflix, Prime Video, HBO Max y Play Store. Diseño negro, 4+3, navegación con control remoto.

## Compilar desde teléfono
1. Crea un repositorio GitHub **privado** llamado MiXiaomiTV.
2. Sube **el contenido** de esta carpeta a la raíz del repositorio (incluye `.github/workflows/android.yml`). Puedes usar la opción de GitHub para subir archivos desde el navegador en modo escritorio; para subir carpetas completas resulta más fácil usar github.dev o Codespaces.
3. En GitHub abre **Actions → Compilar APK Android TV → Run workflow** (si no se ejecutó automáticamente al subir).
4. Al terminar, abre la ejecución y descarga **MiXiaomiTV-APK** en Artifacts. Descomprime el ZIP: contiene `app-debug.apk`.
5. Instala la APK en el televisor mediante Send Files to TV o ADB. Autoriza la instalación desde esa fuente si se solicita.

## Uso y recuperación
- Pulsa OK sobre una tarjeta para abrir su app.
- Mantén pulsado OK sobre una tarjeta para asignar manualmente una aplicación instalada (especialmente IPTV Stream Player o Zapping).
- Botón **Ajustes del televisor** o tecla Menú para abrir ajustes.
- **No deshabilites el launcher original**. Si el sistema pregunta qué app usar como Inicio, prueba primero 'Solo una vez'.
- Para volver atrás: Ajustes → Aplicaciones → Mi Xiaomi TV → Borrar valores predeterminados o Desinstalar (los nombres varían según firmware).

## Límites
- No modifica velocidad del procesador, RAM total ni servicios del sistema.
- El launcher puede no quedar predeterminado según versión de Xiaomi/Google TV.
- Algunas apps no se detectan por nombre o no publican actividad de TV; se pueden asignar manualmente si están visibles en el selector.
- APK de depuración, para uso personal y pruebas; no distribuir como versión final de producción.
