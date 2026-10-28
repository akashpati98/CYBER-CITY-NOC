package com.example.model

enum class VsolWanMode(val label: String) {
    PPPOE("PPPoE Dial-up"),
    IPOE_DHCP("IPoE / DHCP Client"),
    STATIC_IP("Static IP Address"),
    BRIDGE("Bridge Mode")
}

data class VsolRouterConfig(
    val serialNumber: String = "VSOL-9921-A101",
    val model: String = "V2804AX (WiFi 6 GPON)",
    val targetCustomerName: String = "Cyber City Resident #101",
    val wanMode: VsolWanMode = VsolWanMode.PPPOE,
    val pppoeUsername: String = "cyber_vsol_user01",
    val pppoePassword: String = "VsolFiber@Pass2026",
    val staticIp: String = "10.10.25.105",
    val staticSubnet: String = "255.255.255.0",
    val staticGateway: String = "10.10.25.1",
    val staticDns1: String = "8.8.8.8",
    val staticDns2: String = "1.1.1.1",
    val vlanId: Int = 100,
    val vlanPriority: Int = 0,
    val wifiSsid24: String = "CyberCity_Vsol_2.4G",
    val wifiSsid5: String = "CyberCity_Vsol_5G_WiFi6",
    val wifiPassword: String = "VsolWifi#2026",
    val wifiSecurity: String = "WPA2/WPA3-Personal",
    val lanIpAddress: String = "192.168.1.1",
    val dhcpEnabled: Boolean = true,
    val catvRfEnabled: Boolean = true,
    val catvRfPower: String = "78 dBuV",
    val tr069AcsUrl: String = "http://acs.cybercity.net:7547",
    val remoteWanAccess: Boolean = true,
    val lastConfiguredTimestamp: Long = System.currentTimeMillis()
)
