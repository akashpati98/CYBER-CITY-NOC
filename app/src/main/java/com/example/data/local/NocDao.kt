package com.example.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface NocDao {

    // --- OLT Operations ---
    @Query("SELECT * FROM olts ORDER BY name ASC")
    fun getAllOlts(): Flow<List<OltEntity>>

    @Query("SELECT COUNT(*) FROM olts")
    suspend fun getOltCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOlt(olt: OltEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOlts(olts: List<OltEntity>)

    @Query("DELETE FROM olts WHERE id = :oltId")
    suspend fun deleteOltById(oltId: String)

    // --- PON Port Operations ---
    @Query("SELECT * FROM pon_ports ORDER BY name ASC")
    fun getAllPonPorts(): Flow<List<PonPortEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPonPorts(ports: List<PonPortEntity>)

    @Query("DELETE FROM pon_ports WHERE oltId = :oltId")
    suspend fun deletePonPortsByOltId(oltId: String)

    // --- ONU Operations ---
    @Query("SELECT * FROM onus ORDER BY customerName ASC")
    fun getAllOnus(): Flow<List<OnuEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOnu(onu: OnuEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOnus(onus: List<OnuEntity>)

    @Query("UPDATE onus SET rxPowerDbm = :rxPower, statusName = :statusName WHERE id = :onuId")
    suspend fun updateOnuPower(onuId: String, rxPower: Float, statusName: String)

    @Query("DELETE FROM onus WHERE id = :onuId")
    suspend fun deleteOnuById(onuId: String)

    @Query("DELETE FROM onus WHERE oltId = :oltId")
    suspend fun deleteOnusByOltId(oltId: String)

    // --- ONU Power History Operations ---
    @Query("SELECT * FROM onu_power_history ORDER BY timestamp ASC")
    fun getAllOnuPowerHistory(): Flow<List<OnuPowerHistoryEntity>>

    @Query("SELECT * FROM onu_power_history WHERE onuId = :onuId ORDER BY timestamp ASC")
    fun getOnuPowerHistoryForOnu(onuId: String): Flow<List<OnuPowerHistoryEntity>>

    @Query("SELECT COUNT(*) FROM onu_power_history")
    suspend fun getOnuPowerHistoryCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOnuPowerHistory(points: List<OnuPowerHistoryEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOnuPowerHistoryPoint(point: OnuPowerHistoryEntity)

    @Query("DELETE FROM onu_power_history WHERE onuId = :onuId")
    suspend fun deleteOnuPowerHistoryByOnuId(onuId: String)

    // --- MikroTik Operations ---
    @Query("SELECT * FROM mikrotiks ORDER BY name ASC")
    fun getAllMikrotiks(): Flow<List<MikrotikEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMikrotik(mikrotik: MikrotikEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMikrotiks(mikrotiks: List<MikrotikEntity>)

    @Query("DELETE FROM mikrotiks WHERE id = :deviceId")
    suspend fun deleteMikrotikById(deviceId: String)

    // --- Interface Operations ---
    @Query("SELECT * FROM interfaces ORDER BY name ASC")
    fun getAllInterfaces(): Flow<List<NetworkInterfaceEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInterfaces(interfaces: List<NetworkInterfaceEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateInterface(intf: NetworkInterfaceEntity)

    @Query("UPDATE interfaces SET linkStatusName = :status WHERE id = :interfaceId")
    suspend fun updateInterfaceLinkStatus(interfaceId: String, status: String)

    @Query("DELETE FROM interfaces WHERE routerId = :routerId")
    suspend fun deleteInterfacesByRouterId(routerId: String)

    // --- Alert Operations ---
    @Query("SELECT * FROM alerts ORDER BY timestamp DESC")
    fun getAllAlerts(): Flow<List<AlertEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlert(alert: AlertEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlerts(alerts: List<AlertEntity>)

    @Query("UPDATE alerts SET isAcknowledged = 1 WHERE id = :alertId")
    suspend fun acknowledgeAlert(alertId: String)

    @Query("DELETE FROM alerts")
    suspend fun clearAllAlerts()

    // --- Threshold Settings ---
    @Query("SELECT * FROM threshold_settings WHERE id = 1 LIMIT 1")
    fun getThresholdSettings(): Flow<ThresholdSettingsEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertThresholdSettings(settings: ThresholdSettingsEntity)

    // --- ISP Portals Operations (BSNL, Meghbela, GTPL, Cyber City Radius) ---
    @Query("SELECT * FROM isp_portals ORDER BY id ASC")
    fun getAllIspPortals(): Flow<List<IspPortalEntity>>

    @Query("SELECT COUNT(*) FROM isp_portals")
    suspend fun getIspPortalCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIspPortal(portal: IspPortalEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIspPortals(portals: List<IspPortalEntity>)

    @Query("UPDATE isp_portals SET username = :username, password = :password, partnerCode = :partnerCode, notes = :notes, lastUpdatedTimestamp = :timestamp WHERE id = :id")
    suspend fun updateIspCredentials(
        id: String,
        username: String,
        password: String,
        partnerCode: String,
        notes: String,
        timestamp: Long = System.currentTimeMillis()
    )
}
