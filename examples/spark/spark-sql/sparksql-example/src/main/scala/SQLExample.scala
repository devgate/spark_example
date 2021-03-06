package example

// Copy example data to HDFS home directory first:
// hdfs dfs -put $DEV1/examples/example-data/people.json
// hdfs dfs -put $DEV1/examples/example-data/pcodes.json

import org.apache.spark.SparkContext
import org.apache.spark.sql.hive.HiveContext
import org.apache.spark.sql.types._
import org.apache.spark.sql.Row

object SQLExample {
   def main(args: Array[String]) {
   
      val sc = new SparkContext()
      val sqlContext = new HiveContext(sc)
      import sqlContext.implicits._

       // Display the names of tables in the Hive Metastore
       val tables = sqlContext.tableNames()
       tables.foreach(println)

        // Example JSON data source

        val peopleDF = sqlContext.read.json("people.json")
        peopleDF.show
        peopleDF.dtypes.foreach(println)

        // Example Hive table data source
        val webpageDF = sqlContext.read.table("webpage")
        webpageDF.show

        // Example third party data source
        // import com.databricks.spark.avro._
        val accountsAvro = sqlContext.read.format("com.databricks.spark.avro").load("/loudacre/accounts_avro")

        // JDBC example -- these two examples are equivalent  
        val dbaccountsDF = sqlContext.read.format("jdbc").option("url","jdbc:mysql://localhost/loudacre").option("dbtable","accounts").option("user","training").option("password","training").load()
        //dbaccountsDF = sqlContext.read.option("user","training").option("password","training").jdbc("jdbc:mysql://localhost/loudacre","accounts")
        
        // Example actions
        peopleDF.count
        peopleDF.limit(3).show

        // Example queries

        peopleDF.select($"name").show 

        // Other example queries
       peopleDF.select(peopleDF("name"),peopleDF("age")+10).show
       peopleDF.sort(peopleDF("age").desc).show


        // Examples of join
        val pcodesDF = sqlContext.read.json("pcodes.json")
        // basic inner join when column names are equal
        peopleDF.join(pcodesDF, "pcode").show
        // left outer join with equal column names
        peopleDF.join(pcodesDF, Array("pcode"), "left_outer").show
         // create the same DF but with different column name for zip codes
        val zcodesDF = pcodesDF.withColumnRenamed("pcode","zip")
        // inner join with differing column names
        peopleDF.join( zcodesDF, $"pcode" === $"zip").show

         
        // RDD example: convert to (pcode,name) pair RDDs and group by pcode
        val peopleRDD = peopleDF.rdd
        val peopleByPCode = peopleRDD.
          map(row => 
                (row(row.fieldIndex("pcode")),
                 row(row.fieldIndex("name")))).
          groupByKey()


        val schema = StructType( Array (
            StructField("age", IntegerType, true),
            StructField("name", StringType, true),
            StructField("pcode", StringType, true)))

       val rowrdd=sc.parallelize(Array(Row(40, "Abram", "01601"),Row(16,"Lucia","87501")))
       val mydf = sqlContext.createDataFrame(rowrdd,schema)
       mydf.show(2)
       
       
        sc.stop()

   }
}