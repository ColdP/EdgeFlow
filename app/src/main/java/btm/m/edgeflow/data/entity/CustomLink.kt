package btm.m.edgeflow.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "custom_links")
data class CustomLink(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val url: String,
    val iconName: String = "link"
)