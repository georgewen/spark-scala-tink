
# spark3 scala 2.12 google tink
--create cluster

gcloud dataproc clusters create gw-cluster-2544 --region us-west1 --zone us-west1-c --single-node --master-machine-type n1-standard-4 --image-version 2.0-ubuntu18 --optional-components ZOOKEEPER,HBASE --project sound-jigsaw-332323 --initialization-actions gs://goog-dataproc-initialization-actions-us-west1/bigtable/bigtable.sh --metadata bigtable-instance=gw-dataproc-bigtable --properties "spark:spark.jars=gs://gw-dataproc-data-store/jars/hbase-spark-1.0.1-SNAPSHOT.jar"

--run job
gcloud dataproc jobs submit spark  --cluster=gw-cluster-2544 --region=us-west1 --jar=target/spark-scala-encryption-0.0.1-SNAPSHOT.jar

--jars? --packages???


spark-submit   --packages org.apache.hbase.connectors.spark:hbase-spark:1.0.1-SNAPSHOT   --class example.CopyTable   --jars=target/scala-2.12/bigtable-spark-samples-assembly-0.1.jar

https://github.com/apache/hbase-connectors/tree/master/spark
mvn -Dspark.version=3.1.2 -Dscala.version=2.12.14 -Dscala.binary.version=2.12 clean install


-- WINDOWS
spark-class org.apache.spark.deploy.master.Master
spark-class org.apache.spark.deploy.worker.Worker spark://192.168.0.114:7077
spark-shell --master spark://ip:port

https://cloud.google.com/dataproc/docs/guides/manage-spark-dependencies


- start bigtable simulator:
gcloud beta emulators bigtable start
$(gcloud beta emulators bigtable env-init)

--windows
set BIGTABLE_EMULATOR_HOST=127.0.0.1:8086

unset BIGTABLE_EMULATOR_HOST

to hide a pkg from being shaded in maven, add it to the exclusion rule under dependency management (then you need to provide the jar to spark)


