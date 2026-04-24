# NextPlayer — índice rápido de archivos Kotlin

Índice de acceso directo a los archivos `.kt` más visibles del fork. Donde la estructura estaba anidada y GitHub no expuso el nombre del archivo sin abrir más niveles, dejé el enlace a la carpeta para entrar desde ahí.

## app
- [ImageLoaderModule.kt](https://github.com/mhz698a/nextplayer/blob/main/app/src/main/java/dev/anilbeesetti/nextplayer/ImageLoaderModule.kt) — módulo DI para carga de imágenes y miniaturas.
- [MainActivity.kt](https://github.com/mhz698a/nextplayer/blob/main/app/src/main/java/dev/anilbeesetti/nextplayer/MainActivity.kt) — actividad principal de la app.
- [MainViewModel.kt](https://github.com/mhz698a/nextplayer/blob/main/app/src/main/java/dev/anilbeesetti/nextplayer/MainViewModel.kt) — estado y lógica global de la app.
- [NextPlayerApplication.kt](https://github.com/mhz698a/nextplayer/blob/main/app/src/main/java/dev/anilbeesetti/nextplayer/NextPlayerApplication.kt) — clase Application e inicialización global.
- [VideoThumbnailDecoder.kt](https://github.com/mhz698a/nextplayer/blob/main/app/src/main/java/dev/anilbeesetti/nextplayer/VideoThumbnailDecoder.kt) — decodificador para miniaturas de video.
- [CrashActivity.kt](https://github.com/mhz698a/nextplayer/blob/main/app/src/main/java/dev/anilbeesetti/nextplayer/crash/CrashActivity.kt) — pantalla de error/crash.
- [GlobalExceptionHandler.kt](https://github.com/mhz698a/nextplayer/blob/main/app/src/main/java/dev/anilbeesetti/nextplayer/crash/GlobalExceptionHandler.kt) — manejador global de excepciones.
- [MediaNavGraph.kt](https://github.com/mhz698a/nextplayer/blob/main/app/src/main/java/dev/anilbeesetti/nextplayer/navigation/MediaNavGraph.kt) — grafo de navegación del flujo de media.
- [SettingsNavGraph.kt](https://github.com/mhz698a/nextplayer/blob/main/app/src/main/java/dev/anilbeesetti/nextplayer/navigation/SettingsNavGraph.kt) — grafo de navegación de ajustes.

## core/common
- [Logger.kt](https://github.com/mhz698a/nextplayer/blob/main/core/common/src/main/java/dev/anilbeesetti/nextplayer/core/common/Logger.kt) — utilidades de logging.
- [NextDispatchers.kt](https://github.com/mhz698a/nextplayer/blob/main/core/common/src/main/java/dev/anilbeesetti/nextplayer/core/common/NextDispatchers.kt) — abstracción de dispatchers de coroutines.
- [ThumbnailGenerator.kt](https://github.com/mhz698a/nextplayer/blob/main/core/common/src/main/java/dev/anilbeesetti/nextplayer/core/common/ThumbnailGenerator.kt) — generación de miniaturas.
- [Utils.kt](https://github.com/mhz698a/nextplayer/blob/main/core/common/src/main/java/dev/anilbeesetti/nextplayer/core/common/Utils.kt) — utilidades compartidas.
- [di/]( https://github.com/mhz698a/nextplayer/tree/main/core/common/src/main/java/dev/anilbeesetti/nextplayer/core/common/di ) — subcarpeta de inyección de dependencias.
- [extensions/]( https://github.com/mhz698a/nextplayer/tree/main/core/common/src/main/java/dev/anilbeesetti/nextplayer/core/common/extensions ) — extensiones compartidas.

## core/data
- [DataModule.kt](https://github.com/mhz698a/nextplayer/blob/main/core/data/src/main/java/dev/anilbeesetti/nextplayer/core/data/DataModule.kt) — módulo DI de la capa data.
- [mappers/]( https://github.com/mhz698a/nextplayer/tree/main/core/data/src/main/java/dev/anilbeesetti/nextplayer/core/data/mappers ) — mapeadores de datos.
- [models/]( https://github.com/mhz698a/nextplayer/tree/main/core/data/src/main/java/dev/anilbeesetti/nextplayer/core/data/models ) — modelos de persistencia/red.
- [network/]( https://github.com/mhz698a/nextplayer/tree/main/core/data/src/main/java/dev/anilbeesetti/nextplayer/core/data/network ) — fuentes remotas y sincronización.
- [repository/]( https://github.com/mhz698a/nextplayer/tree/main/core/data/src/main/java/dev/anilbeesetti/nextplayer/core/data/repository ) — repositorios de datos.

## core/database
- [schemas/]( https://github.com/mhz698a/nextplayer/tree/main/core/database/schemas/dev.anilbeesetti.nextplayer.core.database.MediaDatabase ) — definición de la base de datos.
- [src/]( https://github.com/mhz698a/nextplayer/tree/main/core/database/src ) — código fuente del módulo de base de datos.

## core/datastore
- [src/]( https://github.com/mhz698a/nextplayer/tree/main/core/datastore/src ) — persistencia tipo DataStore.

## core/domain
- [src/]( https://github.com/mhz698a/nextplayer/tree/main/core/domain/src ) — capa de dominio.

## core/media
- [MediaModule.kt](https://github.com/mhz698a/nextplayer/blob/main/core/media/src/main/java/dev/anilbeesetti/nextplayer/core/media/MediaModule.kt) — módulo DI de media.
- [TextSidecarResolver.kt](https://github.com/mhz698a/nextplayer/blob/main/core/media/src/main/java/dev/anilbeesetti/nextplayer/core/media/TextSidecarResolver.kt) — resolución de subtítulos/sidecar.
- [model/]( https://github.com/mhz698a/nextplayer/tree/main/core/media/src/main/java/dev/anilbeesetti/nextplayer/core/media/model ) — modelos de media.
- [services/]( https://github.com/mhz698a/nextplayer/tree/main/core/media/src/main/java/dev/anilbeesetti/nextplayer/core/media/services ) — servicios de media.
- [sync/]( https://github.com/mhz698a/nextplayer/tree/main/core/media/src/main/java/dev/anilbeesetti/nextplayer/core/media/sync ) — sincronización de media.

## core/model
- [ApplicationPreferences.kt](https://github.com/mhz698a/nextplayer/blob/main/core/model/src/main/java/dev/anilbeesetti/nextplayer/core/model/ApplicationPreferences.kt) — preferencias globales de la app.
- [AudioStreamInfo.kt](https://github.com/mhz698a/nextplayer/blob/main/core/model/src/main/java/dev/anilbeesetti/nextplayer/core/model/AudioStreamInfo.kt) — metadata de pistas de audio.
- [ControlButtonsPosition.kt](https://github.com/mhz698a/nextplayer/blob/main/core/model/src/main/java/dev/anilbeesetti/nextplayer/core/model/ControlButtonsPosition.kt) — posición de botones de control.
- [DecoderPriority.kt](https://github.com/mhz698a/nextplayer/blob/main/core/model/src/main/java/dev/anilbeesetti/nextplayer/core/model/DecoderPriority.kt) — prioridad de decodificadores.
- [DoubleTapGesture.kt](https://github.com/mhz698a/nextplayer/blob/main/core/model/src/main/java/dev/anilbeesetti/nextplayer/core/model/DoubleTapGesture.kt) — configuración de doble tap.
- [FastSeek.kt](https://github.com/mhz698a/nextplayer/blob/main/core/model/src/main/java/dev/anilbeesetti/nextplayer/core/model/FastSeek.kt) — configuración de seek rápido.
- [Folder.kt](https://github.com/mhz698a/nextplayer/blob/main/core/model/src/main/java/dev/anilbeesetti/nextplayer/core/model/Folder.kt) — modelo de carpeta.
- [Font.kt](https://github.com/mhz698a/nextplayer/blob/main/core/model/src/main/java/dev/anilbeesetti/nextplayer/core/model/Font.kt) — configuración de fuente.
- [Journal.kt](https://github.com/mhz698a/nextplayer/blob/main/core/model/src/main/java/dev/anilbeesetti/nextplayer/core/model/Journal.kt) — modelo de jornada.
- [LoopMode.kt](https://github.com/mhz698a/nextplayer/blob/main/core/model/src/main/java/dev/anilbeesetti/nextplayer/core/model/LoopMode.kt) — modo de repetición.
- [MediaLayoutMode.kt](https://github.com/mhz698a/nextplayer/blob/main/core/model/src/main/java/dev/anilbeesetti/nextplayer/core/model/MediaLayoutMode.kt) — modo de layout de medios.
- [MediaViewMode.kt](https://github.com/mhz698a/nextplayer/blob/main/core/model/src/main/java/dev/anilbeesetti/nextplayer/core/model/MediaViewMode.kt) — modo de vista de media.
- [PlayerPreferences.kt](https://github.com/mhz698a/nextplayer/blob/main/core/model/src/main/java/dev/anilbeesetti/nextplayer/core/model/PlayerPreferences.kt) — preferencias del reproductor.
- [Resume.kt](https://github.com/mhz698a/nextplayer/blob/main/core/model/src/main/java/dev/anilbeesetti/nextplayer/core/model/Resume.kt) — estado de reanudación.
- [ScreenOrientation.kt](https://github.com/mhz698a/nextplayer/blob/main/core/model/src/main/java/dev/anilbeesetti/nextplayer/core/model/ScreenOrientation.kt) — orientación de pantalla.
- [SearchHistory.kt](https://github.com/mhz698a/nextplayer/blob/main/core/model/src/main/java/dev/anilbeesetti/nextplayer/core/model/SearchHistory.kt) — historial de búsqueda.
- [SettingsBundle.kt](https://github.com/mhz698a/nextplayer/blob/main/core/model/src/main/java/dev/anilbeesetti/nextplayer/core/model/SettingsBundle.kt) — paquete de ajustes.
- [Sort.kt](https://github.com/mhz698a/nextplayer/blob/main/core/model/src/main/java/dev/anilbeesetti/nextplayer/core/model/Sort.kt) — criterio de orden.
- [SubtitleStreamInfo.kt](https://github.com/mhz698a/nextplayer/blob/main/core/model/src/main/java/dev/anilbeesetti/nextplayer/core/model/SubtitleStreamInfo.kt) — metadata de subtítulos.
- [ThemeConfig.kt](https://github.com/mhz698a/nextplayer/blob/main/core/model/src/main/java/dev/anilbeesetti/nextplayer/core/model/ThemeConfig.kt) — configuración de tema.
- [ThumbnailGenerationStrategy.kt](https://github.com/mhz698a/nextplayer/blob/main/core/model/src/main/java/dev/anilbeesetti/nextplayer/core/model/ThumbnailGenerationStrategy.kt) — estrategia de miniaturas.

## core/ui
- [src/]( https://github.com/mhz698a/nextplayer/tree/main/core/ui/src ) — recursos y código UI del módulo core.

## feature/player
- [MediaPlayerScreen.kt](https://github.com/mhz698a/nextplayer/blob/main/feature/player/src/main/java/dev/anilbeesetti/nextplayer/feature/player/MediaPlayerScreen.kt) — pantalla principal del reproductor.
- [PlayerActivity.kt](https://github.com/mhz698a/nextplayer/blob/main/feature/player/src/main/java/dev/anilbeesetti/nextplayer/feature/player/PlayerActivity.kt) — actividad que orquesta reproducción y flujo de jornadas.
- [PlayerContentFrame.kt](https://github.com/mhz698a/nextplayer/blob/main/feature/player/src/main/java/dev/anilbeesetti/nextplayer/feature/player/PlayerContentFrame.kt) — contenedor visual del player.
- [PlayerViewModel.kt](https://github.com/mhz698a/nextplayer/blob/main/feature/player/src/main/java/dev/anilbeesetti/nextplayer/feature/player/PlayerViewModel.kt) — estado/lógica del player.
- [buttons/]( https://github.com/mhz698a/nextplayer/tree/main/feature/player/src/main/java/dev/anilbeesetti/nextplayer/feature/player/buttons ) — botones del player.
  - [NextButton.kt](https://github.com/mhz698a/nextplayer/blob/main/feature/player/src/main/java/dev/anilbeesetti/nextplayer/feature/player/buttons/NextButton.kt) — botón de siguiente.
  - [PlayPauseButton.kt](https://github.com/mhz698a/nextplayer/blob/main/feature/player/src/main/java/dev/anilbeesetti/nextplayer/feature/player/buttons/PlayPauseButton.kt) — botón play/pausa.
  - [PlayerButton.kt](https://github.com/mhz698a/nextplayer/blob/main/feature/player/src/main/java/dev/anilbeesetti/nextplayer/feature/player/buttons/PlayerButton.kt) — base reutilizable para botones.
  - [PreviousButton.kt](https://github.com/mhz698a/nextplayer/blob/main/feature/player/src/main/java/dev/anilbeesetti/nextplayer/feature/player/buttons/PreviousButton.kt) — botón de anterior.
  - [RepeatButton.kt](https://github.com/mhz698a/nextplayer/blob/main/feature/player/src/main/java/dev/anilbeesetti/nextplayer/feature/player/buttons/RepeatButton.kt) — botón de repetición.
  - [ShuffleButton.kt](https://github.com/mhz698a/nextplayer/blob/main/feature/player/src/main/java/dev/anilbeesetti/nextplayer/feature/player/buttons/ShuffleButton.kt) — botón de mezcla.
- [extensions/]( https://github.com/mhz698a/nextplayer/tree/main/feature/player/src/main/java/dev/anilbeesetti/nextplayer/feature/player/extensions ) — extensiones Kotlin del player.
  - [Activity.kt](https://github.com/mhz698a/nextplayer/blob/main/feature/player/src/main/java/dev/anilbeesetti/nextplayer/feature/player/extensions/Activity.kt) — extensiones de Activity.
  - [Duration.kt](https://github.com/mhz698a/nextplayer/blob/main/feature/player/src/main/java/dev/anilbeesetti/nextplayer/feature/player/extensions/Duration.kt) — extensiones para duraciones.
  - [Enum.kt](https://github.com/mhz698a/nextplayer/blob/main/feature/player/src/main/java/dev/anilbeesetti/nextplayer/feature/player/extensions/Enum.kt) — helpers para enums.
  - [Font.kt](https://github.com/mhz698a/nextplayer/blob/main/feature/player/src/main/java/dev/anilbeesetti/nextplayer/feature/player/extensions/Font.kt) — helpers de tipografía.
  - [ImageButton.kt](https://github.com/mhz698a/nextplayer/blob/main/feature/player/src/main/java/dev/anilbeesetti/nextplayer/feature/player/extensions/ImageButton.kt) — helpers de ImageButton.
  - [MappedTrackInfo.kt](https://github.com/mhz698a/nextplayer/blob/main/feature/player/src/main/java/dev/anilbeesetti/nextplayer/feature/player/extensions/MappedTrackInfo.kt) — extensiones de info de tracks.
  - [MediaItem.kt](https://github.com/mhz698a/nextplayer/blob/main/feature/player/src/main/java/dev/anilbeesetti/nextplayer/feature/player/extensions/MediaItem.kt) — extensiones de MediaItem.
  - [Modifier.kt](https://github.com/mhz698a/nextplayer/blob/main/feature/player/src/main/java/dev/anilbeesetti/nextplayer/feature/player/extensions/Modifier.kt) — extensiones de Modifier.
  - [Player.kt](https://github.com/mhz698a/nextplayer/blob/main/feature/player/src/main/java/dev/anilbeesetti/nextplayer/feature/player/extensions/Player.kt) — extensiones de Player.
  - [PlayerView.kt](https://github.com/mhz698a/nextplayer/blob/main/feature/player/src/main/java/dev/anilbeesetti/nextplayer/feature/player/extensions/PlayerView.kt) — extensiones de PlayerView.
  - [PointerInputScope.kt](https://github.com/mhz698a/nextplayer/blob/main/feature/player/src/main/java/dev/anilbeesetti/nextplayer/feature/player/extensions/PointerInputScope.kt) — helpers de input táctil.
  - [ScreenOrientation.kt](https://github.com/mhz698a/nextplayer/blob/main/feature/player/src/main/java/dev/anilbeesetti/nextplayer/feature/player/extensions/ScreenOrientation.kt) — helpers de orientación.
- [model/]( https://github.com/mhz698a/nextplayer/tree/main/feature/player/src/main/java/dev/anilbeesetti/nextplayer/feature/player/model ) — modelos del player.
  - [Subtitle.kt](https://github.com/mhz698a/nextplayer/blob/main/feature/player/src/main/java/dev/anilbeesetti/nextplayer/feature/player/model/Subtitle.kt) — modelo de subtítulo.
- [service/]( https://github.com/mhz698a/nextplayer/tree/main/feature/player/src/main/java/dev/anilbeesetti/nextplayer/feature/player/service ) — servicios del player.
  - [CustomCommands.kt](https://github.com/mhz698a/nextplayer/blob/main/feature/player/src/main/java/dev/anilbeesetti/nextplayer/feature/player/service/CustomCommands.kt) — comandos personalizados para la sesión de reproducción.
  - [PlayerService.kt](https://github.com/mhz698a/nextplayer/blob/main/feature/player/src/main/java/dev/anilbeesetti/nextplayer/feature/player/service/PlayerService.kt) — servicio de reproducción.
- [state/]( https://github.com/mhz698a/nextplayer/tree/main/feature/player/src/main/java/dev/anilbeesetti/nextplayer/feature/player/state ) — estados UI/playlist/gestos del player.
  - [BrightnessState.kt](https://github.com/mhz698a/nextplayer/blob/main/feature/player/src/main/java/dev/anilbeesetti/nextplayer/feature/player/state/BrightnessState.kt) — estado de brillo.
  - [ControlsVisibilityState.kt](https://github.com/mhz698a/nextplayer/blob/main/feature/player/src/main/java/dev/anilbeesetti/nextplayer/feature/player/state/ControlsVisibilityState.kt) — visibilidad de controles.
  - [CuesState.kt](https://github.com/mhz698a/nextplayer/blob/main/feature/player/src/main/java/dev/anilbeesetti/nextplayer/feature/player/state/CuesState.kt) — estado de cues.
  - [ErrorState.kt](https://github.com/mhz698a/nextplayer/blob/main/feature/player/src/main/java/dev/anilbeesetti/nextplayer/feature/player/state/ErrorState.kt) — estado de error.
  - [MediaPresentationState.kt](https://github.com/mhz698a/nextplayer/blob/main/feature/player/src/main/java/dev/anilbeesetti/nextplayer/feature/player/state/MediaPresentationState.kt) — estado de presentación de media.
  - [MetadataState.kt](https://github.com/mhz698a/nextplayer/blob/main/feature/player/src/main/java/dev/anilbeesetti/nextplayer/feature/player/state/MetadataState.kt) — estado de metadata.
  - [PictureInPictureState.kt](https://github.com/mhz698a/nextplayer/blob/main/feature/player/src/main/java/dev/anilbeesetti/nextplayer/feature/player/state/PictureInPictureState.kt) — estado PiP.
  - [PlaybackParametersState.kt](https://github.com/mhz698a/nextplayer/blob/main/feature/player/src/main/java/dev/anilbeesetti/nextplayer/feature/player/state/PlaybackParametersState.kt) — parámetros de reproducción.
  - [PlaylistState.kt](https://github.com/mhz698a/nextplayer/blob/main/feature/player/src/main/java/dev/anilbeesetti/nextplayer/feature/player/state/PlaylistState.kt) — estado de playlist.
  - [RotationState.kt](https://github.com/mhz698a/nextplayer/blob/main/feature/player/src/main/java/dev/anilbeesetti/nextplayer/feature/player/state/RotationState.kt) — estado de rotación.
  - [SeekGestureState.kt](https://github.com/mhz698a/nextplayer/blob/main/feature/player/src/main/java/dev/anilbeesetti/nextplayer/feature/player/state/SeekGestureState.kt) — estado de gesto de seek.
  - [SubtitleOptionsState.kt](https://github.com/mhz698a/nextplayer/blob/main/feature/player/src/main/java/dev/anilbeesetti/nextplayer/feature/player/state/SubtitleOptionsState.kt) — opciones de subtítulos.
  - [TapGestureState.kt](https://github.com/mhz698a/nextplayer/blob/main/feature/player/src/main/java/dev/anilbeesetti/nextplayer/feature/player/state/TapGestureState.kt) — estado de tap gestures.
  - [TracksState.kt](https://github.com/mhz698a/nextplayer/blob/main/feature/player/src/main/java/dev/anilbeesetti/nextplayer/feature/player/state/TracksState.kt) — estado de tracks.
  - [VideoZoomAndContentScaleState.kt](https://github.com/mhz698a/nextplayer/blob/main/feature/player/src/main/java/dev/anilbeesetti/nextplayer/feature/player/state/VideoZoomAndContentScaleState.kt) — zoom y escala de contenido.
  - [VolumeAndBrightnessGestureState.kt](https://github.com/mhz698a/nextplayer/blob/main/feature/player/src/main/java/dev/anilbeesetti/nextplayer/feature/player/state/VolumeAndBrightnessGestureState.kt) — gestos de volumen/brillo.
  - [VolumeState.kt](https://github.com/mhz698a/nextplayer/blob/main/feature/player/src/main/java/dev/anilbeesetti/nextplayer/feature/player/state/VolumeState.kt) — estado de volumen.
- [ui/]( https://github.com/mhz698a/nextplayer/tree/main/feature/player/src/main/java/dev/anilbeesetti/nextplayer/feature/player/ui ) — vistas Compose del player.
  - [AudioTrackSelectorView.kt](https://github.com/mhz698a/nextplayer/blob/main/feature/player/src/main/java/dev/anilbeesetti/nextplayer/feature/player/ui/AudioTrackSelectorView.kt) — selector de pistas de audio.
  - [DoubleTapIndicator.kt](https://github.com/mhz698a/nextplayer/blob/main/feature/player/src/main/java/dev/anilbeesetti/nextplayer/feature/player/ui/DoubleTapIndicator.kt) — indicador visual de doble tap.
  - [OSDSettingsView.kt](https://github.com/mhz698a/nextplayer/blob/main/feature/player/src/main/java/dev/anilbeesetti/nextplayer/feature/player/ui/OSDSettingsView.kt) — ajustes del OSD.
  - [OverlayShowView.kt](https://github.com/mhz698a/nextplayer/blob/main/feature/player/src/main/java/dev/anilbeesetti/nextplayer/feature/player/ui/OverlayShowView.kt) — vista de overlay.
  - [OverlayView.kt](https://github.com/mhz698a/nextplayer/blob/main/feature/player/src/main/java/dev/anilbeesetti/nextplayer/feature/player/ui/OverlayView.kt) — overlay principal.
  - [PlaybackOverflowMenu.kt](https://github.com/mhz698a/nextplayer/blob/main/feature/player/src/main/java/dev/anilbeesetti/nextplayer/feature/player/ui/PlaybackOverflowMenu.kt) — menú extra del playback.
  - [PlaybackSpeedSelectorView.kt](https://github.com/mhz698a/nextplayer/blob/main/feature/player/src/main/java/dev/anilbeesetti/nextplayer/feature/player/ui/PlaybackSpeedSelectorView.kt) — selector de velocidad.
  - [PlayerGestures.kt](https://github.com/mhz698a/nextplayer/blob/main/feature/player/src/main/java/dev/anilbeesetti/nextplayer/feature/player/ui/PlayerGestures.kt) — gestos del reproductor.
  - [PlaylistView.kt](https://github.com/mhz698a/nextplayer/blob/main/feature/player/src/main/java/dev/anilbeesetti/nextplayer/feature/player/ui/PlaylistView.kt) — vista de playlist.
  - [RadioButtonRow.kt](https://github.com/mhz698a/nextplayer/blob/main/feature/player/src/main/java/dev/anilbeesetti/nextplayer/feature/player/ui/RadioButtonRow.kt) — fila reutilizable de radio buttons.
  - [ShutterView.kt](https://github.com/mhz698a/nextplayer/blob/main/feature/player/src/main/java/dev/anilbeesetti/nextplayer/feature/player/ui/ShutterView.kt) — shutter visual.
  - [SubtitleSelectorView.kt](https://github.com/mhz698a/nextplayer/blob/main/feature/player/src/main/java/dev/anilbeesetti/nextplayer/feature/player/ui/SubtitleSelectorView.kt) — selector de subtítulos.
  - [SubtitleView.kt](https://github.com/mhz698a/nextplayer/blob/main/feature/player/src/main/java/dev/anilbeesetti/nextplayer/feature/player/ui/SubtitleView.kt) — render de subtítulos.
  - [VerticalProgressView.kt](https://github.com/mhz698a/nextplayer/blob/main/feature/player/src/main/java/dev/anilbeesetti/nextplayer/feature/player/ui/VerticalProgressView.kt) — barra vertical de progreso.
  - [VideoContentScaleSelectorView.kt](https://github.com/mhz698a/nextplayer/blob/main/feature/player/src/main/java/dev/anilbeesetti/nextplayer/feature/player/ui/VideoContentScaleSelectorView.kt) — selector de escala de contenido.
  - [controls/]( https://github.com/mhz698a/nextplayer/tree/main/feature/player/src/main/java/dev/anilbeesetti/nextplayer/feature/player/ui/controls ) — controles superiores e inferiores.
    - [ControlsBottomView.kt](https://github.com/mhz698a/nextplayer/blob/main/feature/player/src/main/java/dev/anilbeesetti/nextplayer/feature/player/ui/controls/ControlsBottomView.kt) — controles inferiores.
    - [ControlsTopView.kt](https://github.com/mhz698a/nextplayer/blob/main/feature/player/src/main/java/dev/anilbeesetti/nextplayer/feature/player/ui/controls/ControlsTopView.kt) — controles superiores.
- [utils/]( https://github.com/mhz698a/nextplayer/tree/main/feature/player/src/main/java/dev/anilbeesetti/nextplayer/feature/player/utils ) — utilidades del player.
  - [PlayerApi.kt](https://github.com/mhz698a/nextplayer/blob/main/feature/player/src/main/java/dev/anilbeesetti/nextplayer/feature/player/utils/PlayerApi.kt) — API auxiliar del flujo de reproducción.

## feature/settings
- [SettingsScreen.kt](https://github.com/mhz698a/nextplayer/blob/main/feature/settings/src/main/java/dev/anilbeesetti/nextplayer/settings/SettingsScreen.kt) — pantalla principal de ajustes.
- [composables/OptionsDialog.kt](https://github.com/mhz698a/nextplayer/blob/main/feature/settings/src/main/java/dev/anilbeesetti/nextplayer/settings/composables/OptionsDialog.kt) — diálogo reutilizable de opciones.
- [extensions/]( https://github.com/mhz698a/nextplayer/tree/main/feature/settings/src/main/java/dev/anilbeesetti/nextplayer/settings/extensions ) — extensiones de settings.
  - [ControlButtonsPosition.kt](https://github.com/mhz698a/nextplayer/blob/main/feature/settings/src/main/java/dev/anilbeesetti/nextplayer/settings/extensions/ControlButtonsPosition.kt) — posición de botones de control.
  - [DecoderPriority.kt](https://github.com/mhz698a/nextplayer/blob/main/feature/settings/src/main/java/dev/anilbeesetti/nextplayer/settings/extensions/DecoderPriority.kt) — prioridad de decodificador.
  - [DoubleTapGesture.kt](https://github.com/mhz698a/nextplayer/blob/main/feature/settings/src/main/java/dev/anilbeesetti/nextplayer/settings/extensions/DoubleTapGesture.kt) — configuración de doble tap.
  - [FastSeek.kt](https://github.com/mhz698a/nextplayer/blob/main/feature/settings/src/main/java/dev/anilbeesetti/nextplayer/settings/extensions/FastSeek.kt) — configuración de seek rápido.
  - [Font.kt](https://github.com/mhz698a/nextplayer/blob/main/feature/settings/src/main/java/dev/anilbeesetti/nextplayer/settings/extensions/Font.kt) — ajustes de fuente.
  - [Resume.kt](https://github.com/mhz698a/nextplayer/blob/main/feature/settings/src/main/java/dev/anilbeesetti/nextplayer/settings/extensions/Resume.kt) — configuración de reanudación.
  - [ScreenOrientation.kt](https://github.com/mhz698a/nextplayer/blob/main/feature/settings/src/main/java/dev/anilbeesetti/nextplayer/settings/extensions/ScreenOrientation.kt) — orientación de pantalla.
  - [ThemeConfig.kt](https://github.com/mhz698a/nextplayer/blob/main/feature/settings/src/main/java/dev/anilbeesetti/nextplayer/settings/extensions/ThemeConfig.kt) — configuración de tema.
- [navigation/]( https://github.com/mhz698a/nextplayer/tree/main/feature/settings/src/main/java/dev/anilbeesetti/nextplayer/settings/navigation ) — navegación interna de preferencias.
  - [AboutPreferencesNavigation.kt](https://github.com/mhz698a/nextplayer/blob/main/feature/settings/src/main/java/dev/anilbeesetti/nextplayer/settings/navigation/AboutPreferencesNavigation.kt) — navegación a About.
  - [AppearancePreferencesNavigation.kt](https://github.com/mhz698a/nextplayer/blob/main/feature/settings/src/main/java/dev/anilbeesetti/nextplayer/settings/navigation/AppearancePreferencesNavigation.kt) — navegación a Appearance.
  - [AudioPreferencesNavigation.kt](https://github.com/mhz698a/nextplayer/blob/main/feature/settings/src/main/java/dev/anilbeesetti/nextplayer/settings/navigation/AudioPreferencesNavigation.kt) — navegación a Audio.
  - [DecoderPreferencesNavigation.kt](https://github.com/mhz698a/nextplayer/blob/main/feature/settings/src/main/java/dev/anilbeesetti/nextplayer/settings/navigation/DecoderPreferencesNavigation.kt) — navegación a Decoder.
  - [GeneralPreferencesNavigation.kt](https://github.com/mhz698a/nextplayer/blob/main/feature/settings/src/main/java/dev/anilbeesetti/nextplayer/settings/navigation/GeneralPreferencesNavigation.kt) — navegación a General.
  - [GesturePreferencesNavigation.kt](https://github.com/mhz698a/nextplayer/blob/main/feature/settings/src/main/java/dev/anilbeesetti/nextplayer/settings/navigation/GesturePreferencesNavigation.kt) — navegación a Gesture.
- [screens/]( https://github.com/mhz698a/nextplayer/tree/main/feature/settings/src/main/java/dev/anilbeesetti/nextplayer/settings/screens ) — pantallas de ajustes por categoría.
  - [about/](https://github.com/mhz698a/nextplayer/tree/main/feature/settings/src/main/java/dev/anilbeesetti/nextplayer/settings/screens/about) — pantallas de información y librerías.
  - [appearance/](https://github.com/mhz698a/nextplayer/tree/main/feature/settings/src/main/java/dev/anilbeesetti/nextplayer/settings/screens/appearance) — tema y apariencia.
  - [audio/](https://github.com/mhz698a/nextplayer/tree/main/feature/settings/src/main/java/dev/anilbeesetti/nextplayer/settings/screens/audio) — preferencias de audio.
  - [decoder/](https://github.com/mhz698a/nextplayer/tree/main/feature/settings/src/main/java/dev/anilbeesetti/nextplayer/settings/screens/decoder) — preferencias de decodificación.
  - [general/](https://github.com/mhz698a/nextplayer/tree/main/feature/settings/src/main/java/dev/anilbeesetti/nextplayer/settings/screens/general) — ajustes generales.
  - [gesture/](https://github.com/mhz698a/nextplayer/tree/main/feature/settings/src/main/java/dev/anilbeesetti/nextplayer/settings/screens/gesture) — gestos.
  - [journals/](https://github.com/mhz698a/nextplayer/tree/main/feature/settings/src/main/java/dev/anilbeesetti/nextplayer/settings/screens/journals) — ajustes relacionados con jornadas.
  - [medialibrary/](https://github.com/mhz698a/nextplayer/tree/main/feature/settings/src/main/java/dev/anilbeesetti/nextplayer/settings/screens/medialibrary) — biblioteca multimedia.
  - [player/](https://github.com/mhz698a/nextplayer/tree/main/feature/settings/src/main/java/dev/anilbeesetti/nextplayer/settings/screens/player) — ajustes del reproductor.
  - [subtitle/](https://github.com/mhz698a/nextplayer/tree/main/feature/settings/src/main/java/dev/anilbeesetti/nextplayer/settings/screens/subtitle) — ajustes de subtítulos.
  - [thumbnail/](https://github.com/mhz698a/nextplayer/tree/main/feature/settings/src/main/java/dev/anilbeesetti/nextplayer/settings/screens/thumbnail) — ajustes de miniaturas.
- [utils/]( https://github.com/mhz698a/nextplayer/tree/main/feature/settings/src/main/java/dev/anilbeesetti/nextplayer/settings/utils ) — utilidades de settings.
  - [LocalesHelper.kt](https://github.com/mhz698a/nextplayer/blob/main/feature/settings/src/main/java/dev/anilbeesetti/nextplayer/settings/utils/LocalesHelper.kt) — ayuda para locales/idiomas.
- [SettingsScreen.kt](https://github.com/mhz698a/nextplayer/blob/main/feature/settings/src/main/java/dev/anilbeesetti/nextplayer/settings/SettingsScreen.kt) — host de la sección de ajustes.

## feature/videopicker
- [composables/]( https://github.com/mhz698a/nextplayer/tree/main/feature/videopicker/src/main/java/dev/anilbeesetti/nextplayer/feature/videopicker/composables ) — componentes Compose de selección.
  - [FolderItem.kt](https://github.com/mhz698a/nextplayer/blob/main/feature/videopicker/src/main/java/dev/anilbeesetti/nextplayer/feature/videopicker/composables/FolderItem.kt) — item de carpeta.
  - [InfoChip.kt](https://github.com/mhz698a/nextplayer/blob/main/feature/videopicker/src/main/java/dev/anilbeesetti/nextplayer/feature/videopicker/composables/InfoChip.kt) — chip informativo.
  - [MediaContent.kt](https://github.com/mhz698a/nextplayer/blob/main/feature/videopicker/src/main/java/dev/anilbeesetti/nextplayer/feature/videopicker/composables/MediaContent.kt) — contenido multimedia.
  - [MediaView.kt](https://github.com/mhz698a/nextplayer/blob/main/feature/videopicker/src/main/java/dev/anilbeesetti/nextplayer/feature/videopicker/composables/MediaView.kt) — vista multimedia.
  - [QuickSettingsDialog.kt](https://github.com/mhz698a/nextplayer/blob/main/feature/videopicker/src/main/java/dev/anilbeesetti/nextplayer/feature/videopicker/composables/QuickSettingsDialog.kt) — diálogo de ajustes rápidos.
- [extensions/]( https://github.com/mhz698a/nextplayer/tree/main/feature/videopicker/src/main/java/dev/anilbeesetti/nextplayer/feature/videopicker/extensions ) — extensiones de la feature videopicker.
- [navigation/]( https://github.com/mhz698a/nextplayer/tree/main/feature/videopicker/src/main/java/dev/anilbeesetti/nextplayer/feature/videopicker/navigation ) — navegación de videopicker.
  - [MediaPickerNavigation.kt](https://github.com/mhz698a/nextplayer/blob/main/feature/videopicker/src/main/java/dev/anilbeesetti/nextplayer/feature/videopicker/navigation/MediaPickerNavigation.kt) — navegación del selector de media.
  - [SearchNavigation.kt](https://github.com/mhz698a/nextplayer/blob/main/feature/videopicker/src/main/java/dev/anilbeesetti/nextplayer/feature/videopicker/navigation/SearchNavigation.kt) — navegación de búsqueda.
- [screens/journals/]( https://github.com/mhz698a/nextplayer/tree/main/feature/videopicker/src/main/java/dev/anilbeesetti/nextplayer/feature/videopicker/screens/journals ) — pantalla de detalle/lista de jornadas.
  - [JournalDetailScreen.kt](https://github.com/mhz698a/nextplayer/blob/main/feature/videopicker/src/main/java/dev/anilbeesetti/nextplayer/feature/videopicker/screens/journals/JournalDetailScreen.kt) — detalle de jornada.
  - [JournalDetailState.kt](https://github.com/mhz698a/nextplayer/blob/main/feature/videopicker/src/main/java/dev/anilbeesetti/nextplayer/feature/videopicker/screens/journals/JournalDetailState.kt) — estado del detalle.
  - [JournalDetailViewModel.kt](https://github.com/mhz698a/nextplayer/blob/main/feature/videopicker/src/main/java/dev/anilbeesetti/nextplayer/feature/videopicker/screens/journals/JournalDetailViewModel.kt) — viewmodel del detalle.
  - [JournalsListScreen.kt](https://github.com/mhz698a/nextplayer/blob/main/feature/videopicker/src/main/java/dev/anilbeesetti/nextplayer/feature/videopicker/screens/journals/JournalsListScreen.kt) — lista de jornadas.
  - [JournalsListViewModel.kt](https://github.com/mhz698a/nextplayer/blob/main/feature/videopicker/src/main/java/dev/anilbeesetti/nextplayer/feature/videopicker/screens/journals/JournalsListViewModel.kt) — viewmodel de jornadas.
- [screens/mediapicker/]( https://github.com/mhz698a/nextplayer/tree/main/feature/videopicker/src/main/java/dev/anilbeesetti/nextplayer/feature/videopicker/screens/mediapicker ) — selector de archivos multimedia.
  - [MediaPickerScreen.kt](https://github.com/mhz698a/nextplayer/blob/main/feature/videopicker/src/main/java/dev/anilbeesetti/nextplayer/feature/videopicker/screens/mediapicker/MediaPickerScreen.kt) — pantalla de selector de media.
  - [MediaPickerViewModel.kt](https://github.com/mhz698a/nextplayer/blob/main/feature/videopicker/src/main/java/dev/anilbeesetti/nextplayer/feature/videopicker/screens/mediapicker/MediaPickerViewModel.kt) — viewmodel del selector.
- [screens/search/]( https://github.com/mhz698a/nextplayer/tree/main/feature/videopicker/src/main/java/dev/anilbeesetti/nextplayer/feature/videopicker/screens/search ) — búsqueda de media/jornadas.
  - [SearchScreen.kt](https://github.com/mhz698a/nextplayer/blob/main/feature/videopicker/src/main/java/dev/anilbeesetti/nextplayer/feature/videopicker/screens/search/SearchScreen.kt) — pantalla de búsqueda.
  - [SearchViewModel.kt](https://github.com/mhz698a/nextplayer/blob/main/feature/videopicker/src/main/java/dev/anilbeesetti/nextplayer/feature/videopicker/screens/search/SearchViewModel.kt) — viewmodel de búsqueda.
- [MediaState.kt](https://github.com/mhz698a/nextplayer/blob/main/feature/videopicker/src/main/java/dev/anilbeesetti/nextplayer/feature/videopicker/screens/MediaState.kt) — estado compartido del picker.
- [screens/]( https://github.com/mhz698a/nextplayer/tree/main/feature/videopicker/src/main/java/dev/anilbeesetti/nextplayer/feature/videopicker/screens ) — contenedor de pantallas adicionales.

## Raíz del repo
- [AGENTS.md](https://github.com/mhz698a/nextplayer/blob/main/AGENTS.md) — guía para agentes que modifiquen el repo.
- [README.md](https://github.com/mhz698a/nextplayer/blob/main/README.md) — documentación principal.
- [settings.gradle.kts](https://github.com/mhz698a/nextplayer/blob/main/settings.gradle.kts) — configuración de módulos Gradle.
- [build.gradle.kts](https://github.com/mhz698a/nextplayer/blob/main/build.gradle.kts) — build raíz.
