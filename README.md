# eGESVEN — Configuración del proyecto

Prototipo funcional del sistema de gestión de ventas eGESVEN.
Aplicación Java sobre base de datos Oracle, con arquitectura en capas
(Presentación → Aplicación → Dominio / Infraestructura).

---

## 1. Requisitos

| Herramienta | Versión | Notas |
|---|---|---|
| **Java (JDK)** | 17 | El `pom.xml` compila con `source/target 17`. |
| **Maven** | 3.8 o superior | Para compilar y ejecutar. |
| **Oracle Database** | 12c o superior | El esquema usa columnas `GENERATED ALWAYS AS IDENTITY`. |
| **Docker** | cualquiera | Solo en macOS y Linux (ver punto 2). |

---

## 2. Levantar Oracle

La aplicación espera encontrar Oracle en `localhost:1521`, en la pluggable
database **`FREEPDB1`**. Elige la vía según tu sistema operativo.

### Windows — instalación nativa (no requiere Docker)

1. Descarga **Oracle Database Free** desde el sitio oficial de Oracle
   (versión Windows x64) e instálalo. Cualquier versión reciente sirve.
2. Durante la instalación define la contraseña de `SYS` / `SYSTEM`.
3. El instalador crea automáticamente el PDB `FREEPDB1` y deja el listener en
   el puerto 1521, que es justo lo que espera el proyecto.

Oracle queda registrado como servicio de Windows y arranca solo al encender el
equipo: no hay que activarlo cada vez.

> Si instalas **Oracle XE** en lugar de **Free**, el PDB se llama `XEPDB1`, no
> `FREEPDB1`. En ese caso hay que ajustar la URL en `Conexion.java` (ver punto 5).

### macOS / Linux — con Docker

Oracle no publica instalador nativo para macOS, así que se usa un contenedor:

```bash
docker run -d --name oracle-free \
  -p 1521:1521 \
  -e ORACLE_PASSWORD=oraclepass \
  container-registry.oracle.com/database/free:latest-lite
```

Espera a que la base termine de inicializar (la primera vez tarda 1-2 minutos):

```bash
docker logs -f oracle-free      # espera "DATABASE IS READY TO USE!", luego Ctrl+C
```

Puedes confirmar que está lista con `docker ps`: la columna `STATUS` debe decir
`healthy`.

**En arranques posteriores** el contenedor no se levanta solo. Para reiniciarlo:

```bash
docker start oracle-free        # start, NO run: run crearia un contenedor nuevo y vacio
```

> Si usas **Colima** en lugar de Docker Desktop, primero arranca la máquina
> virtual con `colima start`.

---

## 3. Crear el usuario de la base de datos

**Este paso es obligatorio.** El script `eGESVEN_oracle.sql` crea las tablas
pero **no** crea el usuario dueño del esquema, así que hay que crearlo a mano
una sola vez.

Las credenciales deben coincidir exactamente con las de `Conexion.java`:
usuario **`egesven`**, contraseña **`egesven123`**.

**macOS / Linux (Docker):**

```bash
docker exec -it oracle-free sqlplus sys/oraclepass@localhost:1521/FREEPDB1 as sysdba
```

**Windows (instalación nativa):**

```
sqlplus sys/TU_PASSWORD@localhost:1521/FREEPDB1 as sysdba
```

Y dentro de SQL*Plus, en cualquiera de los dos casos:

```sql
CREATE USER egesven IDENTIFIED BY egesven123;
GRANT CONNECT, RESOURCE, DBA TO egesven;
EXIT;
```

---

## 4. Cargar el esquema

Ejecuta el script del proyecto, que crea las 11 tablas, los índices y los datos
de prueba:

**Windows** (o cualquier sistema con SQL*Plus instalado localmente):

```
sqlplus egesven/egesven123@localhost:1521/FREEPDB1 @eGESVEN_oracle.sql
```

**macOS / Linux** (SQL*Plus vive dentro del contenedor):

```bash
docker cp eGESVEN_oracle.sql oracle-free:/tmp/
docker exec -i oracle-free sqlplus egesven/egesven123@localhost:1521/FREEPDB1 @/tmp/eGESVEN_oracle.sql
```

Al terminar, el script lista las tablas creadas. Deben aparecer 11.

> El script empieza con sentencias `DROP TABLE`. Es intencional: permite
> re-ejecutarlo para dejar la base en su estado inicial. La primera vez los
> `DROP` fallan con `ORA-00942` (tabla inexistente) y **eso es normal**.

---

## 5. Datos de conexión

Están definidos en `src/main/java/cl/egesven/infraestructura/Conexion.java`:

```java
URL      = "jdbc:oracle:thin:@localhost:1521/FREEPDB1"
USER     = "egesven"
PASSWORD = "egesven123"
```

Si seguiste los pasos anteriores con los valores por defecto, **no hay que
tocar nada**. Solo requiere ajuste si cambiaste el puerto del listener o si
usas Oracle XE (donde el PDB se llama `XEPDB1`).

---

## 6. Ejecutar la aplicación

### Desde IntelliJ IDEA

Abre `src/main/java/cl/egesven/web/AppWeb.java` y pulsa el botón ▶️ junto a
`public static void main`, o clic derecho → **Run 'AppWeb.main()'**.

### Desde la terminal

```bash
mvn compile exec:java
```

En ambos casos la aplicación queda disponible en:

**http://localhost:4567**

---

## 7. Usuarios de prueba

| Usuario | Contraseña | Perfil |
|---|---|---|
| `admin` | `admin123` | Administrador |
| `jperez` | `cliente123` | Cliente |

El inicio de sesión pide un **segundo factor (MFA)**. Como es un prototipo, el
código de verificación no se envía por SMS: aparece en pantalla y también en la
consola donde corre la aplicación.

> Los datos de prueba del script traen los hashes como texto de relleno
> (`HASH_DEMO_*`). La aplicación los reemplaza por hashes reales al arrancar,
> por eso las contraseñas de la tabla funcionan desde el primer inicio.

---

## 8. Problemas frecuentes

| Síntoma | Causa y solución |
|---|---|
| `No se pudo conectar a Oracle` | La base no está corriendo. En Docker: `docker start oracle-free` y espera a que aparezca `healthy` en `docker ps`. |
| `ORA-01017: invalid username/password` | El usuario `egesven` no existe o tiene otra contraseña. Repite el punto 3. |
| `ORA-00942: table or view does not exist` | El esquema no se cargó. Repite el punto 4. (Si sale solo en los `DROP` iniciales del script, ignóralo.) |
| `Address already in use` al arrancar | El puerto 4567 está ocupado por otra instancia. En macOS/Linux: `lsof -ti:4567 \| xargs kill -9`. |
| El listener no responde en macOS | Si usas Colima, la VM está apagada: `colima start`. |
| Las imágenes de productos no cargan | Falta `src/main/resources/public/img/producto.svg`. Verifica que el archivo esté en el repo. |

---

## 9. Estructura del proyecto

```
src/main/java/cl/egesven/
├── presentacion/     Pantallas de consola (PantallaLogin, PantallaProductos, ...)
├── web/              AppWeb: servidor HTTP y API REST
├── aplicacion/       Servicios: orquestan los casos de uso
├── dominio/          Entidades y reglas de negocio
└── infraestructura/  Repositorios JDBC, gateways de pago y Logger

src/main/resources/public/    Interfaz web (HTML, CSS, JS e imágenes)
eGESVEN_oracle.sql            Esquema y datos de prueba de la base de datos
```

La dependencia entre capas es unidireccional y descendente: el **Dominio no
depende de Infraestructura**.

Hay dos interfaces de usuario sobre la misma capa de Aplicación: la web
(`AppWeb`, la que se usa en la demo) y una de consola (`Menu`), que se ejecuta
con `cl.egesven.presentacion.Menu`.
