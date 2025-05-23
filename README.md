# Distribuidora de Alimentos - App de Seguimiento y Pedidos

Este proyecto es una aplicación móvil Android desarrollada como parte del curso "Taller de Aplicaciones Móviles". La aplicación está diseñada para una empresa de distribución de alimentos y se enfoca en funcionalidades clave como el registro de usuarios, cálculo de costos de despacho, visualización de ubicaciones en mapa y simulación de monitoreo de temperatura para productos refrigerados.

## Características Principales

* **Autenticación de Usuarios:**
    * Registro de nuevos usuarios mediante correo electrónico y contraseña[cite: 3, 8].
    * Inicio de sesión con correo electrónico y contraseña[cite: 3].
    * Inicio de sesión e integración con cuentas de Google (SSO)[cite: 3].
* **Cálculo de Costo de Despacho:**
    * Permite al usuario ingresar el monto total de su compra[cite: 1, 2, 3].
    * Calcula la distancia desde la ubicación del usuario (obtenida por GPS o ingresada manualmente como dirección) hasta una ubicación de bodega predefinida.
    * Aplica reglas de negocio para determinar el costo del despacho basándose en el monto de la compra y la distancia[cite: 1, 2, 3].
        * Compras sobre $50,000 dentro de 20 km: Despacho gratuito.
        * Compras entre $25,000 y $49,999 dentro de 20 km: Tarifa de $150 por kilómetro.
        * Compras menores a $25,000 dentro de 20 km: Tarifa de $300 por kilómetro.
* **Integración con Mapas:**
    * Muestra la ubicación actual del usuario en un mapa de Google Maps[cite: 2, 3].
    * Muestra la ubicación de la bodega en el mapa[cite: 2].
    * Permite al usuario ingresar una dirección de despacho manualmente, la cual es geocodificada para obtener coordenadas.
* **Monitoreo de Temperatura (Simulado):**
    * Simula la lectura de datos de temperatura de un sensor (como los de un camión refrigerado)[cite: 1, 4, 5].
    * Muestra la temperatura actual en la interfaz.
    * Indica visualmente si la temperatura está en un rango bajo, ideal o alto utilizando colores (azul, verde, rojo).
    * Genera una alerta (Toast) si la temperatura simulada excede los límites predefinidos.
    * Los rangos de temperatura son configurables (actualmente hardcodeados en la app para la simulación).
* **Persistencia de Datos:**
    * Utiliza Firebase Realtime Database para almacenar información relevante como la posición GPS del usuario [cite: 3] y los datos de temperatura simulada[cite: 4, 5].
* **Gestión de Sesión:**
    * Permite al usuario cerrar sesión.

## Tecnologías Utilizadas

* **Lenguaje:** Kotlin [cite: 1]
* **Plataforma:** Android
* **IDE:** Android Studio
* **Autenticación:** Firebase Authentication (Email/Password y Google Sign-In) [cite: 3]
* **Base de Datos en la Nube:** Firebase Realtime Database [cite: 3, 4, 5]
* **Mapas y Geolocalización:** Google Maps Platform (Maps SDK for Android, Fused Location Provider API) [cite: 2]
* **Simulación de Hardware (Contexto del Proyecto):** Se menciona el uso de Tinkercad con Arduino y sensor de temperatura como parte del concepto general del proyecto para la recolección de datos de temperatura[cite: 1, 4, 5].

## Estructura del Proyecto (Módulos Principales)

* `LoginActivity.kt`: Maneja el inicio de sesión y la opción de registrarse.
* `RegisterActivity.kt`: Maneja el registro de nuevos usuarios con correo y contraseña.
* `MenuActivity.kt`: Pantalla principal después del login. Incluye:
    * Visualización del mapa.
    * Entrada de monto de compra y dirección.
    * Cálculo y visualización del costo de despacho.
    * Simulación y visualización del monitoreo de temperatura.
    * Botón de cierre de sesión.

## Requisitos Previos para Ejecutar

* Android Studio (versión compatible con las herramientas utilizadas, ej. Meerkat o posterior).
* Un dispositivo Android o emulador configurado con Google Play Services.
* Configuración de un proyecto Firebase con:
    * Autenticación (Email/Password y Google Sign-In habilitados).
    * Realtime Database.
    * Archivo `google-services.json` añadido al módulo `app` del proyecto.
* Una API Key de Google Maps configurada correctamente en el proyecto de Google Cloud Platform y añadida al `AndroidManifest.xml`.

## Instrucciones de Compilación y Ejecución

1.  Clonar el repositorio: `git clone [URL_DEL_REPOSITORIO]`
2.  Abrir el proyecto en Android Studio.
3.  Asegurarse de que el archivo `google-services.json` (descargado desde tu consola de Firebase) esté en la carpeta `app/`.
4.  Añadir tu API Key de Google Maps en el archivo `app/src/main/AndroidManifest.xml`.
5.  Sincronizar el proyecto con los archivos Gradle.
6.  Ejecutar la aplicación en un dispositivo o emulador.

## Funcionalidades Futuras (Opcional - si aplica)

* Implementación completa del proceso de compra de productos.
* Integración real con sensores de temperatura vía Bluetooth o similar.
* Notificaciones push para alertas de temperatura.
* Roles de usuario (ej. administrador para configurar rangos de temperatura).
* Mejoras en la interfaz de usuario y experiencia de usuario.
