# CMS - Camera Management System
## Build & Run Instructions

### Prerequisites

- Java 17 or higher (JDK)
- Maven 3.8+
- MySQL 8.0+
- Operating System: Windows 10/11, macOS, or Linux

### Step 1: Database Setup

1. Start MySQL server
2. Create the database:

```sql
CREATE DATABASE cms_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

3. Run the schema script:

```bash
mysql -u root -p cms_db < schema.sql
```

### Step 2: Configure Database Connection

Create the config file at `~/.cms/cms.properties`:

```properties
db.url=jdbc:mysql://localhost:3306/cms_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
db.username=root
db.password=your_password_here
```

### Step 3: Build

```bash
mvn clean package -DskipTests
```

### Step 4: Run

```bash
cd cms-app/target
java -jar cms-app-1.0.0.jar
```

Or via Maven:

```bash
cd cms-app
mvn javafx:run
```

### Default Login Credentials

| Role    | Username | Password    |
|---------|----------|-------------|
| Admin   | admin    | admin123    |
| Manager | manager  | manager123  |

### Adding an NVR Device

1. Login with admin or manager credentials
2. Click **+ Add NVR** in the toolbar
3. Fill in:
   - **Location Name**: e.g., "Warehouse NVR"
   - **Brand**: Select your NVR brand (Hikvision, Dahua, CP Plus, ONVIF)
   - **IP Address**: e.g., 192.168.1.64
   - **Port**: Leave blank for default (80 for HTTP, 8000 for Hikvision SDK)
   - **Username/Password**: NVR login credentials
4. Click **Connect**

### Supported NVR Brands & Protocols

| Brand         | Protocol       | Default Port |
|---------------|----------------|-------------|
| Hikvision     | ONVIF + RTSP   | 80 / 8000   |
| Dahua         | ONVIF + RTSP   | 80 / 37777  |
| CP Plus       | ONVIF + RTSP   | 80 / 37777  |
| ONVIF Generic | ONVIF          | 80          |
| Generic HTTP  | WebView        | 80          |

### RTSP Stream URL Formats

- **Hikvision**: `rtsp://user:pass@IP:554/Streaming/Channels/101`
- **Dahua**: `rtsp://user:pass@IP:554/cam/realmonitor?channel=1&subtype=0`
- **ONVIF**: Discovered automatically via ONVIF profiles

### Troubleshooting

**Database connection error**: 
- Verify MySQL is running: `systemctl status mysql`
- Check credentials in `~/.cms/cms.properties`
- Ensure `cms_db` database exists

**NVR connection failed**:
- Verify the IP address is reachable: `ping 192.168.1.x`
- Check NVR has ONVIF enabled (most modern NVRs support it)
- Try the correct port (common: 80, 8000, 37777)

**No stream displayed**:
- RTSP streams require a network route to the NVR
- Check if the NVR's RTSP port (554) is accessible
- Some NVRs require RTSP authentication to be enabled

**Logs**: 
- Located at: `~/.cms/logs/cms.log`
