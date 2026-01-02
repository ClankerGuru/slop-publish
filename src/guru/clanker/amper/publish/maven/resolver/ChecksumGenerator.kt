package guru.clanker.amper.publish.maven.resolver

import guru.clanker.amper.publish.domain.model.Checksum
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest

object ChecksumGenerator {

    fun generate(file: Path): Checksum {
        val bytes = Files.readAllBytes(file)
        return Checksum(
            md5 = bytes.md5Hex(),
            sha1 = bytes.sha1Hex(),
            sha256 = bytes.sha256Hex()
        )
    }

    private fun ByteArray.md5Hex(): String =
        MessageDigest.getInstance("MD5").digest(this).toHexString()

    private fun ByteArray.sha1Hex(): String =
        MessageDigest.getInstance("SHA-1").digest(this).toHexString()

    private fun ByteArray.sha256Hex(): String =
        MessageDigest.getInstance("SHA-256").digest(this).toHexString()

    private fun ByteArray.toHexString(): String =
        joinToString("") { "%02x".format(it) }
}
