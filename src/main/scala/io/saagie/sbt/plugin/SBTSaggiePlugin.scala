package io.saagie.sbt.plugin

import io.saagie.sbt.plugin.enum.JobType
import org.slf4j
import org.slf4j.LoggerFactory
import sbt.{AutoPlugin, Def, _}

/**
  * Created by aurelien on 30/09/16.
  */

case class JobSettings(urlApi: String,
                       login: String,
                       password: String,
                       platformId: String,
                       jobId: Int,
                       jobName: String,
                       jobType: String,
                       jobCategory: String,
                       javaVersion: String,
                       sparkVersion: String,
                       cpu: Double,
                       mem: Int,
                       disk: Int,
                       streaming: Boolean,
                       targetDirectory: String,
                       jarName: String,
                       mainClass: String,
                       arguments: String,
                       description: String,
                       releaseNote: String
                      )

object SBTSaggiePlugin extends AutoPlugin {

  def generateURLJob(urlApi: String, platformId: String, jobId: Int): String = {
    urlApi.replace("/api/v1", "/#/manager/" + platformId + "/job/" + jobId)
  }

  def create(jobSettings: JobSettings): Unit = {

    val saagieClient = new SaagieClient(jobSettings)
    try {
      saagieClient.getManagerStatus

      val filename = saagieClient.uploadFile(jobSettings.targetDirectory, jobSettings.jarName)
      logger.info("filename created " + filename)
      val jobId = jobSettings.jobType match {
        case JobType.JAVA_SCALA.value =>
          val body = "{\"platform_id\":\"" + jobSettings.platformId + "\",\"capsule_code\":\"" + jobSettings.jobType + "\",\"category\":\"" + jobSettings.jobCategory + "\",\"current\":{\"template\":\"java -jar {file} " + jobSettings.arguments + "\",\"file\":\"" + filename + "\",\"options\":{\"language_version\":\"" + jobSettings.javaVersion + "\"},\"cpu\":" + jobSettings.cpu + ",\"memory\":" + jobSettings.mem + ",\"disk\":" + jobSettings.disk + ",\"releaseNote\":\"" + jobSettings.releaseNote + "\" },\"description\":\"" + jobSettings.description + "\",\"manual\":true,\"name\":\"" + jobSettings.jobName + "\",\"retry\":\"\",\"schedule\":\"R0/2016-07-06T15:47:52.051Z/P0Y0M1DT0H0M0S\"}"
          saagieClient.createJob(body)
        case JobType.SPARK.value =>
          val body = "{\"platform_id\":\"" + jobSettings.platformId + "\",\"capsule_code\":\"" + jobSettings.jobType + "\",\"category\":\"" + jobSettings.jobCategory + "\",\"current\":{\"template\":\"spark-submit --class=" + jobSettings.mainClass + " {file} " + jobSettings.arguments + "\",\"file\":\"" + filename + "\",\"options\":{\"language_version\":\"" + jobSettings.sparkVersion + "\", \"extra_language\": \"java\", \"extra_version\":\"" + jobSettings.javaVersion + "\"},\"cpu\":" + jobSettings.cpu + ",\"memory\":" + jobSettings.mem + ",\"disk\":" + jobSettings.disk + ",\"releaseNote\":\"" + jobSettings.releaseNote + "\" },\"description\":\"" + jobSettings.description + "\",\"manual\":true,\"name\":\"" + jobSettings.jobName + "\"," + (if (jobSettings.streaming) "\"streaming\":true," else "") + "\"retry\":\"\",\"schedule\":\"R0/2016-07-06T15:47:52.051Z/P0Y0M1DT0H0M0S\"}"
          saagieClient.createJob(body)
        case _ => throw AutoPluginException("jobType must be " + JobType.JAVA_SCALA.value + " or " + JobType.SPARK.value)
      }
      logger.info("  >> Job created : " + generateURLJob(jobSettings.urlApi, jobSettings.platformId, jobId))
    } finally {
      saagieClient.close()
    }
  }

  def update(jobSettings: JobSettings): Unit = {
    val saagieClient = new SaagieClient(jobSettings)
    try {
      saagieClient.checkManagerConnection()
      val job = saagieClient.checkJobExists()
      val fileName = saagieClient.uploadFile(jobSettings.targetDirectory, jobSettings.jarName)
      val updatedJob = Job(job.id, job.name, job.category, Current(job.current.id, job.current.job_id, job.current.number, job.current.template, fileName, job.current.creation_date, jobSettings.releaseNote, jobSettings.cpu, jobSettings.mem, jobSettings.disk), jobSettings.streaming)
      saagieClient.updateJob(updatedJob)
      logger.info("  >> Job updated : " + generateURLJob(jobSettings.urlApi, jobSettings.platformId, jobSettings.jobId))
    } finally {
      saagieClient.close()
    }
  }

  def logger: slf4j.Logger = LoggerFactory.getLogger(this.getClass)

  object autoImport {

    val urlApi = settingKey[String]("Represents the URL of your manager.")
    val login = settingKey[String]("Represents the login you'll use to have access to your manager.")
    val password = settingKey[String]("Represents the password you'll use to have access to your manager.")
    val platformId = settingKey[String]("Represents the id of the platform you want to add the job.")
    val jobName = settingKey[String]("Represents the name of the job you want to create or to update.")
    val jobType = settingKey[String]("Represents the type of the job you want to create or to update.")
    val jobCategory = settingKey[String]("Represents the category of the job you want to create or to update.")
    val jobId = settingKey[Int]("Represents the id the job you want to update.")
    val javaVersion = settingKey[String]("Represents the version of language you want to run your job.")
    val sparkVersion = settingKey[String]("Represents the version of spark you want to run your job.")
    val cpu = settingKey[Double]("Represents the amount of CPU you want to reserve for your job.")
    val mem = settingKey[Int]("Represents the amount of memory you want to reserve for your job.")
    val disk = settingKey[Int]("Represents the amount of disk space you want to reserve for your job.")
    val streaming = settingKey[Boolean]("Represents if the job is a streaming job or not.")
    var targetDirectory = settingKey[String]("Represents the path for finding your jar.")
    val jar = settingKey[String]("Represents the jar name.")
    val mainClazz = settingKey[String]("Represents the main class of the job you want to create or update.")
    val arguments = settingKey[String]("Represents the arguments of the job you want to create or update.")
    val desc = settingKey[String]("Represents the description of the job you want to create or update.")
    val releaseNote = settingKey[String]("Represents the release note of the job version you want to create or update.")
    val createSaagieJob = taskKey[Unit]("Create Job Task")
    val updateSaagieJob = taskKey[Unit]("Update Job Task")

    lazy val commonSettings: Seq[Def.Setting[_]] = Seq(
      urlApi in createSaagieJob := "https://manager.prod.saagie.io/api/v1",
      login in createSaagieJob := "",
      password in createSaagieJob := "",
      platformId in createSaagieJob := "",
      jobName in createSaagieJob := "",
      jobType in createSaagieJob := "java-scala",
      jobCategory in createSaagieJob := "extract",
      jobId in updateSaagieJob := 0,
      javaVersion in createSaagieJob := "8.121",
      sparkVersion in createSaagieJob := "2.1.0",
      cpu in createSaagieJob := 0.5,
      mem in createSaagieJob := 512,
      disk in createSaagieJob := 1024,
      streaming in createSaagieJob := false,
      targetDirectory in createSaagieJob := "",
      jar in createSaagieJob := "",
      mainClazz in createSaagieJob := "",
      arguments in createSaagieJob := "",
      desc in createSaagieJob := "",
      releaseNote in createSaagieJob := ""
    )

    lazy val createJobSettings: Seq[Def.Setting[_]] = commonSettings ++ Seq(
      createSaagieJob := create(JobSettings((urlApi in createSaagieJob).value, (login in createSaagieJob).value,
        (password in createSaagieJob).value, (platformId in createSaagieJob).value, (jobId in updateSaagieJob).value,
        (jobName in createSaagieJob).value, (jobType in createSaagieJob).value, (jobCategory in createSaagieJob).value,
        (javaVersion in createSaagieJob).value, (sparkVersion in createSaagieJob).value, (cpu in createSaagieJob).value,
        (mem in createSaagieJob).value, (disk in createSaagieJob).value, (streaming in createSaagieJob).value,
        (targetDirectory in createSaagieJob).value, (jar in createSaagieJob).value, (mainClazz in createSaagieJob).value,
        (arguments in createSaagieJob).value, (desc in createSaagieJob).value, (releaseNote in createSaagieJob).value))
    )

    lazy val updateJobSettings: Seq[Def.Setting[_]] = commonSettings ++ Seq(
      updateSaagieJob := update(JobSettings((urlApi in createSaagieJob).value, (login in createSaagieJob).value,
        (password in createSaagieJob).value, (platformId in createSaagieJob).value, (jobId in updateSaagieJob).value,
        (jobName in createSaagieJob).value, (jobType in createSaagieJob).value, (jobCategory in createSaagieJob).value,
        (javaVersion in createSaagieJob).value, (sparkVersion in createSaagieJob).value, (cpu in createSaagieJob).value,
        (mem in createSaagieJob).value, (disk in createSaagieJob).value, (streaming in createSaagieJob).value,
        (targetDirectory in createSaagieJob).value, (jar in createSaagieJob).value, (mainClazz in createSaagieJob).value,
        (arguments in createSaagieJob).value, (desc in createSaagieJob).value, (releaseNote in createSaagieJob).value))
    )
  }

  import autoImport._

  override def trigger: PluginTrigger = allRequirements

  override val projectSettings: Seq[Def.Setting[_]] = createJobSettings ++ updateJobSettings

}
