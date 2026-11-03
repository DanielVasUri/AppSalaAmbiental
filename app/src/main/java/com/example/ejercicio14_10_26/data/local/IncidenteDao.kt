package com.example.ejercicio14_10_26.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface IncidenteDao {

    @Insert
    suspend fun insertar(incidente: Incidente): Long

    @Update
    suspend fun actualizar(incidente: Incidente)

    @Query("SELECT * FROM incidentes WHERE fechaHoraFin IS NULL LIMIT 1")
    suspend fun obtenerIncidenteAbierto(): Incidente?

    @Query("SELECT * FROM incidentes ORDER BY fechaHoraInicio DESC")
    suspend fun obtenerTodos(): List<Incidente>
}
