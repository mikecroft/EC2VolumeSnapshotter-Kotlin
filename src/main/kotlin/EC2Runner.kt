package io.mikecroft.aws

import com.amazonaws.services.ec2.AmazonEC2
import com.amazonaws.services.ec2.AmazonEC2ClientBuilder
import com.amazonaws.services.ec2.model.*
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.KotlinModule
import mu.KotlinLogging
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

/**
 * Created by mike on 05/06/17.
 */

private val logger = KotlinLogging.logger {}
val ec2: AmazonEC2 = AmazonEC2ClientBuilder.defaultClient()

fun main(args: Array<String>) {
    args[0]?.let {
        for ((name, volId, minSnapshots) in readProps(Paths.get(args[0]))) {
            logger.info { "Attempting backup of $name" }
            runBackup(name, volId, minSnapshots)
        }
    }
}

private fun readProps(path: Path): Array<BackupRecord> {
    val mapper = ObjectMapper(YAMLFactory())
    mapper.registerModule(KotlinModule())
    return Files.newBufferedReader(path).use {
        mapper.readValue(it, Array<BackupRecord>::class.java)
    }
}

private fun runBackup(name: String, volId: String, minSnapshots: Int) {
    try {
        createSnapshot(name, volId)

        logger.debug { "Are there more than $minSnapshots snapshots?" }
        if (tooManySnapshots(ec2.describeSnapshots(DescribeSnapshotsRequest()), volId, minSnapshots)) {
            logger.debug { "Found more than $minSnapshots!" }

            val oldSnapId = findOldestSnapshot(ec2.describeSnapshots(DescribeSnapshotsRequest()), volId)?.snapshotId

            logger.debug { "Trying to delete oldest snapshot, ID $oldSnapId" }
            deleteSnapshot(oldSnapId)
        } else {
            logger.debug { "Didn't find more than $minSnapshots" }
        }
    } catch (e: AmazonEC2Exception) {
        notify("The BackupRecord $volId was not recognised! Check the log to see what happened", "red")
        logger.error { e.localizedMessage }
    }
}

data class BackupRecord(val name: String, val volId: String, val minSnapshots: Int)

private fun deleteSnapshot(oldSnapId: String?): DeleteSnapshotResult = ec2.deleteSnapshot(DeleteSnapshotRequest().withSnapshotId(oldSnapId))
private fun findOldestSnapshot(snapshotResult: DescribeSnapshotsResult, volId: String): Snapshot? = snapshotResult.snapshots.filter { it.volumeId == volId }.minBy { it.startTime }
private fun tooManySnapshots(snapshotResult: DescribeSnapshotsResult, volId: String, minSnapshots: Int = 1): Boolean = snapshotResult.snapshots.filter { it.volumeId == volId }.count() > minSnapshots
private fun createSnapshot(name: String, volId: String): CreateSnapshotResult = ec2.createSnapshot(CreateSnapshotRequest().withVolumeId(volId).withDescription("Automated backup for $name"))
private fun notify(message: String, colour: String) { //TODO Need to add a notification alert for Slack/Hipchat
}