import org.apache.hadoop.hbase.spark.datasources.{HBaseSparkConf, HBaseTableCatalog}
import org.apache.spark.sql.types.{IntegerType, StringType, StructField, StructType}
import org.apache.spark.sql.functions._

import com.google.crypto.tink.Aead
import com.google.crypto.tink.CleartextKeysetHandle
import com.google.crypto.tink.JsonKeysetReader
import com.google.crypto.tink.JsonKeysetWriter
import com.google.crypto.tink.KeyTemplates
import com.google.crypto.tink.KeysetHandle
import com.google.crypto.tink.aead._
import com.google.crypto.tink.aead.AeadConfig._
import java.io._
import java.io.FileOutputStream._
import java.io.IOException._
import java.nio.file.Files._
import java.nio.file.Paths._
import java.security.GeneralSecurityException
import java.util.Base64

//working with spark3

object cryptodemo extends App {

  //println("this is a test")
  
  val appName = this.getClass.getSimpleName.replace("$", "")
  println(s"$appName Spark application is starting up...")

  //val (projectId, instanceId, fromTable, toTable) = parse(args)
  val projectId = "sound-jigsaw-332323"
  val instanceId = "gw-dataproc-bigtable"
  val toTable = "copytable"
   

  import org.apache.spark.sql.SparkSession
  val spark = SparkSession.builder().config("spark.hadoop.validateOutputSpecs", false).getOrCreate()
  println(s"Spark version: ${spark.version}")
  import spark.implicits._


  import com.google.cloud.bigtable.hbase.BigtableConfiguration
  val conf = BigtableConfiguration.configure(projectId, instanceId)
  import org.apache.hadoop.hbase.spark.HBaseContext

  new HBaseContext(spark.sparkContext, conf)


  def createCatalogJSON(table: String): String = {
    s"""{
       |"table":{"namespace":"default", "name":"$table", "tableCoder":"PrimitiveType"},
       |"rowkey":"recordid",
       |"columns":{
       |  "recordid":{"cf":"rowkey", "col":"recordid", "type":"string"},
       |  "responseid":{"cf":"cf", "col":"responseid", "type":"string"},
       |  "name":{"cf":"cf", "col":"name", "type":"string"},
       |  "Response":{"cf":"cf2", "col":"Response", "type":"string"}
       |}
       |}""".stripMargin
  }


val encryptString = (inputStr:String)  => 
  { 

  val  keyContent = "{\"primaryKeyId\":2036321936,\"key\":[{\"keyData\":{\"typeUrl\":\"type.googleapis.com/google.crypto.tink.AesGcmKey\",\"value\":\"GhDmBgtFdbVUjpfBSY+w6xhY\",\"keyMaterialType\":\"SYMMETRIC\"},\"status\":\"ENABLED\",\"keyId\":2036321936,\"outputPrefixType\":\"TINK\"}]}";
	val keyBytes = keyContent.getBytes();

  ///val kekUri = "gcp-kms://projects/sound-jigsaw-332323/locations/us-central1/keyRings/mygcpkmskey/cryptoKeys/tink"
  //val gcpCredentialFilename = "gcp-kms-credential.json"
  AeadConfig.register
  //val keyFileName = "gcp-keyset.json"
  //val keyFile = new File(keyFileName)
  var handle: KeysetHandle = null
  val EMPTY_ASSOCIATED_DATA = new Array[Byte](0)

    try {
        handle = CleartextKeysetHandle.read(JsonKeysetReader.withBytes(keyBytes))
    }
    catch {
      case ex@(_: GeneralSecurityException | _: IOException) =>
        println("Error reading key: " + ex)
       System.exit(1)
    }
    // Get the primitive
    var aead: Aead = null
    try aead = handle.getPrimitive(classOf[Aead])
    catch {
      case ex: GeneralSecurityException =>
        System.err.println("Error creating primitive: %s " + ex)
        System.exit(1)
    }
    val inputBytes = inputStr.getBytes()
    val ciphertext = aead.encrypt(inputBytes, EMPTY_ASSOCIATED_DATA)
    val encryptText: String = Base64.getEncoder.encodeToString(ciphertext)
    encryptText
  }
  

  var df = spark.read.format("csv").option("header","true").load("gs://gw-dataproc-data-store/*.txt")
  //var df = spark.read.format("csv").option("header","true").load("data/*.txt")

  //df = df.select($"recordid",$"name",$"responseid",concat(col("responseid") ,lit(".xml")).alias("filename"))
  df = df.withColumn("filename",concat($"responseid" ,lit(".xml")))
  df.show()

  //df.repartitionby('filename')

  var df2 = spark.read.format("csv").load("gs://gw-dataproc-data-store/*.xml")
  df2 = df2.withColumn("filename",regexp_replace(input_file_name(), "gs://gw-dataproc-data-store/", "")).withColumnRenamed("_c0","Response0")
  //var df2 = spark.read.format("csv").load("data/*.xml")
  //df2 = df2.withColumn("filename",regexp_replace(input_file_name(), "file:///home/george/eclipse-workspace/spark-scala-encryption/data/", "")).withColumnRenamed("_c0","Response0")

  df2.show()

  //df2.repartitionby('filename')

//  val fullDf = df2.join(df, df("filename") === df2("filename"),"inner")//.drop("filename2").drop("filename")
  var fullDf = df2.join(df,"filename").drop("filename")

  val encryptUDF = udf(encryptString)
  //fullDf = fullDf.withColumn("Response",encryptUDF(col("Response0"))).drop("Response0")

  val respString = encryptString("this is a test")
  println(respString)

  fullDf = fullDf.withColumn("Response",encryptUDF(col("Response0"))).drop("Response0")

  fullDf.show()

  println(s"Writing records to $toTable")
  fullDf
    .write
     //// .options(Map(HBaseTableCatalog.tableCatalog->createCatalogJSON(toTable)))
     ////   .format("org.apache.spark.sql.execution.datasources.hbase")    
    .format("org.apache.hadoop.hbase.spark")
    .option(HBaseTableCatalog.tableCatalog, createCatalogJSON(toTable))
    .save
  println(s"Writing to $toTable...DONE")
  

}