package helpers.crypto

import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.Paths
import com.google.crypto.tink.{Aead, CleartextKeysetHandle, JsonKeysetReader, KeysetHandle}
import com.google.crypto.tink.aead.AeadConfig
import com.google.crypto.tink.integration.awskms.AwsKmsClient
import com.typesafe.config.ConfigFactory


object Crypter {

  AeadConfig.register()
  val conf = ConfigFactory.load()
  val plainKeysetFilePath = conf.getString("enc.plainKeyset")
  val encKeysetFilePath = conf.getString("enc.encKeyset")
  val masterKeyUri = conf.getString("enc.kmsUri")

  val aead = keysetHandle().getPrimitive[Aead](classOf[Aead])


  def encrypt(plainText: String, associatedData: String) = {
    aead.encrypt(plainText.getBytes(), associatedData.getBytes())
  }


  def decrypt(ciphertext: Array[Byte], associatedData: String) = {
    val decryptedArrayOfBytes = aead.decrypt(ciphertext, associatedData.getBytes)
    // DO NOT USE: decryptedArrayOfBytes.map(_.toChar).mkString
    // It doesn't work with Japanese.
    new String(decryptedArrayOfBytes, StandardCharsets.UTF_8)
  }

  def keysetHandle(): KeysetHandle = {
    // TEST: no test for both cases. Only one test is possible for now
    //   Hard to get both tests because the condition depends on configuration file.
    //   But both cases work fine. Just no test.
    if (masterKeyUri.isEmpty) {
      CleartextKeysetHandle.read(JsonKeysetReader.withFile(new File(plainKeysetFilePath)))
    }
    else {
      KeysetHandle.read(JsonKeysetReader.withFile(new File(encKeysetFilePath)), new AwsKmsClient().getAead(masterKeyUri))
    }

  }


}
