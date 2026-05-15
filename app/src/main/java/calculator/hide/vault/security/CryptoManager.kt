package calculator.hide.vault.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Manages AES-GCM encryption/decryption using Android Keystore
 * File format: "VAULT1" (6 bytes) + ivLength (4 bytes) + iv (12 bytes) + cipherBytes
 */
class CryptoManager(private val context: Context) {
    
    companion object {
        private const val KEYSTORE_ALIAS = "VaultKey"
        private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_TAG_LENGTH = 128 // bits
        private const val IV_LENGTH = 12 // bytes
        private const val HEADER = "VAULT1"
    }
    
    private val keyStore: KeyStore = KeyStore.getInstance(KEYSTORE_PROVIDER).apply {
        load(null)
    }
    
    /**
     * Get or create the encryption key from Android Keystore
     */
    private fun getOrCreateKey(): SecretKey {
        if (!keyStore.containsAlias(KEYSTORE_ALIAS)) {
            val keyGenerator = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES,
                KEYSTORE_PROVIDER
            )
            val keyGenParameterSpec = KeyGenParameterSpec.Builder(
                KEYSTORE_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .build()
            
            keyGenerator.init(keyGenParameterSpec)
            keyGenerator.generateKey()
        }
        
        return keyStore.getKey(KEYSTORE_ALIAS, null) as SecretKey
    }
    
    /**
     * Encrypt data and save to file
     * @param plainData The plain data to encrypt
     * @param outputFile The file to save encrypted data
     */
    fun encrypt(plainData: ByteArray, outputFile: File) {
        val secretKey = getOrCreateKey()
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)
        
        val iv = cipher.iv
        val encryptedData = cipher.doFinal(plainData)
        
        // Write: HEADER (6) + ivLength (4) + iv (12) + cipherBytes
        FileOutputStream(outputFile).use { fos ->
            fos.write(HEADER.toByteArray())
            fos.write(ByteBuffer.allocate(4).putInt(iv.size).array())
            fos.write(iv)
            fos.write(encryptedData)
        }
    }
    
    /**
     * Decrypt data from file
     * @param encryptedFile The encrypted file
     * @return Decrypted data
     */
    fun decrypt(encryptedFile: File): ByteArray {
        val secretKey = getOrCreateKey()
        val cipher = Cipher.getInstance(TRANSFORMATION)
        
        FileInputStream(encryptedFile).use { fis ->
            // Read header
            val headerBytes = ByteArray(HEADER.length)
            fis.read(headerBytes)
            if (String(headerBytes) != HEADER) {
                throw IllegalArgumentException("Invalid vault file format")
            }
            
            // Read IV length
            val ivLengthBytes = ByteArray(4)
            fis.read(ivLengthBytes)
            val ivLength = ByteBuffer.wrap(ivLengthBytes).int
            
            // Read IV
            val iv = ByteArray(ivLength)
            fis.read(iv)
            
            // Read encrypted data
            val encryptedData = fis.readBytes()
            
            // Decrypt
            val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmSpec)
            return cipher.doFinal(encryptedData)
        }
    }
    
    /**
     * Decrypt to temporary file (for preview)
     * @param encryptedFile The encrypted file
     * @return Temporary file with decrypted data
     */
    fun decryptToTempFile(encryptedFile: File): File {
        val decryptedData = decrypt(encryptedFile)
        val tempFile = File.createTempFile("vault_preview_", null, context.cacheDir)
        FileOutputStream(tempFile).use { it.write(decryptedData) }
        return tempFile
    }
    
    /**
     * Stream encryption: Encrypt input stream to file (for large files)
     * @param inputStream The input stream to encrypt
     * @param outputFile The file to save encrypted data
     */
    fun encryptStreamToFile(inputStream: java.io.InputStream, outputFile: File) {
        val secretKey = getOrCreateKey()
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)
        
        val iv = cipher.iv
        
        FileOutputStream(outputFile).use { fos ->
            // Write header: HEADER (6) + ivLength (4) + iv (12)
            fos.write(HEADER.toByteArray())
            fos.write(ByteBuffer.allocate(4).putInt(iv.size).array())
            fos.write(iv)
            
            // Stream encryption with buffer
            val buffer = ByteArray(8192) // 8KB buffer
            var bytesRead: Int
            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                val encrypted = cipher.update(buffer, 0, bytesRead)
                if (encrypted != null && encrypted.isNotEmpty()) {
                    fos.write(encrypted)
                }
            }
            
            // Finalize encryption
            val finalEncrypted = cipher.doFinal()
            if (finalEncrypted != null && finalEncrypted.isNotEmpty()) {
                fos.write(finalEncrypted)
            }
        }
    }
    
    /**
     * Stream decryption: Decrypt file to output stream (for large files)
     * @param encryptedFile The encrypted file
     * @param outputStream The output stream to write decrypted data
     */
    fun decryptFileToOutputStream(encryptedFile: File, outputStream: java.io.OutputStream) {
        val secretKey = getOrCreateKey()
        val cipher = Cipher.getInstance(TRANSFORMATION)
        
        FileInputStream(encryptedFile).use { fis ->
            // Read header
            val headerBytes = ByteArray(HEADER.length)
            fis.read(headerBytes)
            if (String(headerBytes) != HEADER) {
                throw IllegalArgumentException("Invalid vault file format")
            }
            
            // Read IV length
            val ivLengthBytes = ByteArray(4)
            fis.read(ivLengthBytes)
            val ivLength = ByteBuffer.wrap(ivLengthBytes).int
            
            // Read IV
            val iv = ByteArray(ivLength)
            fis.read(iv)
            
            // Initialize decryption
            val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmSpec)
            
            // Stream decryption with buffer
            val buffer = ByteArray(8192) // 8KB buffer
            var bytesRead: Int
            while (fis.read(buffer).also { bytesRead = it } != -1) {
                val decrypted = cipher.update(buffer, 0, bytesRead)
                if (decrypted != null && decrypted.isNotEmpty()) {
                    outputStream.write(decrypted)
                }
            }
            
            // Finalize decryption
            val finalDecrypted = cipher.doFinal()
            if (finalDecrypted != null && finalDecrypted.isNotEmpty()) {
                outputStream.write(finalDecrypted)
            }
        }
    }
}

