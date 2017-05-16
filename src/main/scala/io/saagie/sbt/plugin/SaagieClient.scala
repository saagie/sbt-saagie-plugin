package io.saagie.sbt.plugin

import java.io.File
import java.nio.file.{Files, Paths}

import org.slf4j.LoggerFactory
import play.api.http.{HeaderNames, Writeable}
import play.api.libs.ws.WSAuthScheme
import play.api.libs.ws.ning.NingWSClient
import play.api.mvc.MultipartFormData.FilePart
import play.api.mvc.{Codec, MultipartFormData}
import sbt.AutoPluginException

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext}


case class Job(id: Int, name: String, category: String, current: Current, streaming: Boolean)

case class Current(id: Int, job_id: Int, number: Int, template: String, file: String, creation_date: String, releaseNote: String, cpu: Double, memory: Int, disk: Int)

object MultipartFormDataWritable {

  implicit val context: ExecutionContext = play.api.libs.concurrent.Execution.Implicits.defaultContext

  val boundary = "--------ABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890"

  private def formatDataParts(data: Map[String, Seq[String]]) = {
    val dataParts = data.flatMap { case (key, values) =>
      values.map { value =>
        val name = s""""$key""""
        s"--$boundary\r\n${HeaderNames.CONTENT_DISPOSITION}: form-data; name=$name\r\n\r\n$value\r\n"
      }
    }.mkString("")
    Codec.utf_8.encode(dataParts)
  }

  private def filePartHeader(file: FilePart[File]) = {
    val name = s""""${file.key}""""
    val filename = s""""${file.filename}""""
    val contentType = file.contentType.map { ct =>
      s"${HeaderNames.CONTENT_TYPE}: $ct\r\n"
    }.getOrElse("")
    Codec.utf_8.encode(s"--$boundary\r\n${HeaderNames.CONTENT_DISPOSITION}: form-data; name=$name; filename=$filename\r\n$contentType\r\n")
  }

  val singleton: Writeable[MultipartFormData[File]] = Writeable[MultipartFormData[File]](
    transform = { form: MultipartFormData[File] =>
      (formatDataParts(form.dataParts) ++
        form.files.flatMap { (file: FilePart[File]) =>
          val fileBytes = Files.readAllBytes(Paths.get(file.ref.getAbsolutePath))
          filePartHeader(file) ++ fileBytes ++ Codec.utf_8.encode("\r\n")
        }) ++ Codec.utf_8.encode(s"--$boundary--")
    },
    contentType = Some(s"multipart/form-data; boundary=$boundary")
  )
}


class SaagieClient(jobSettings: JobSettings) {

  private val logger = LoggerFactory.getLogger(this.getClass)

  val ws = NingWSClient()

  import play.api.libs.functional.syntax._
  import play.api.libs.json._

  implicit val context: ExecutionContext = play.api.libs.concurrent.Execution.Implicits.defaultContext

  implicit val anyContentAsMultipartFormWritable: Writeable[MultipartFormData[File]] = {
    MultipartFormDataWritable.singleton
  }

  implicit val currentReads: Reads[Current] = (
    (JsPath \ "id").readNullable[Int].map(_.getOrElse(0)) and
      (JsPath \ "job_id").readNullable[Int].map(_.getOrElse(0)) and
      (JsPath \ "number").readNullable[Int].map(_.getOrElse(0)) and
      (JsPath \ "template").read[String] and
      (JsPath \ "file").read[String] and
      (JsPath \ "creation_date").readNullable[String].map(_.getOrElse("")) and
      (JsPath \ "releaseNote").readNullable[String].map(_.getOrElse("")) and
      (JsPath \ "cpu").readNullable[Double].map(_.getOrElse(0d)) and
      (JsPath \ "memory").readNullable[Int].map(_.getOrElse(0)) and
      (JsPath \ "disk").readNullable[Int].map(_.getOrElse(0))
    ) (Current.apply _)

  implicit val jobReads: Reads[Job] = (
    (JsPath \ "id").read[Int] and
      (JsPath \ "name").read[String] and
      (JsPath \ "category").read[String] and
      (JsPath \ "current").read[Current] and
      (JsPath \ "streaming").readNullable[Boolean].map(_.getOrElse(false))
    ) (Job.apply _)


  implicit val currentWrites = new Writes[Current] {
    def writes(current: Current): JsObject = Json.obj(
      "id" -> current.id.intValue(),
      "job_id" -> current.job_id.intValue(),
      "number" -> current.number.intValue(),
      "template" -> current.template,
      "file" -> current.file,
      "creation_date" -> current.creation_date,
      "releaseNote" -> current.releaseNote,
      "cpu" -> current.cpu,
      "memory" -> current.memory,
      "disk" -> current.disk
    )
  }

  implicit val jobWrites = new Writes[Job] {
    def writes(job: Job): JsObject = Json.obj(
      "id" -> job.id,
      "name" -> job.name,
      "category" -> job.category,
      "current" -> job.current,
      "streaming" -> job.streaming
    )
  }

  def close(): Unit = {
    ws.close()
  }


  def checkManagerConnection() {
    logger.debug("Check Manager Connection for url " + jobSettings.urlApi + "/platform/" + jobSettings.platformId)
    ws.url(jobSettings.urlApi + "/platform/" + jobSettings.platformId)
      .withAuth(jobSettings.login, jobSettings.password, WSAuthScheme.BASIC)
      .get()
      .map { response =>
        if (response.status != 200) {
          logger.error("Error during check SaagieManager connection(ErrorCode : " + response.status + " )")
          throw AutoPluginException("Error during check SaagieManager connection")
        } else {
          logger.info("Connection to Manager : OK")
        }
      }
  }

  def getManagerStatus: Int = {
    logger.debug("Check Manager status for for url " + jobSettings.urlApi + "/platform/" + jobSettings.platformId)
    val getStatus = ws.url(jobSettings.urlApi + "/platform/" + jobSettings.platformId)
      .withAuth(jobSettings.login, jobSettings.password, WSAuthScheme.BASIC)
      .get()
      .map(_.status)
    Await.result(getStatus, 10 seconds)
  }


  def uploadFile(directory: String, fileName: String): String = {
    val file = Paths.get(directory, fileName).toFile
    val fileParts = Seq(play.api.mvc.MultipartFormData.FilePart("file", fileName, Some("form-data"), file))
    val multipartFormData = MultipartFormData.apply(Map.empty[String, Seq[String]], fileParts, Seq(), Seq())
    //option + Try catch
    val createdFilename = ws.url(jobSettings.urlApi + "/platform/" + jobSettings.platformId + "/job/upload")
      .withAuth(jobSettings.login, jobSettings.password, WSAuthScheme.BASIC)
      .post(multipartFormData)
      .map { response =>
        if (response.status != 200) {
          logger.error("Error during upload file (ErrorCode : " + response.status + " )")
          //Future.failed(AutoPluginException("Error during job upload"))
          throw AutoPluginException("Error during job upload")
        } else {
          logger.info("  >> Upload File OK")
          (response.json \ "fileName").as[String]
        }
      }
    Await.result(createdFilename, 60 seconds)
  }

  def createJob(body: String): Int = {
    logger.info("Create job")
    val id = ws.url(jobSettings.urlApi + "/platform/" + jobSettings.platformId + "/job")
      .withAuth(jobSettings.login, jobSettings.password, WSAuthScheme.BASIC)
      .post(body)
      .map { response =>
        if (response.status != 200) {
          logger.error("Error during create job(ErrorCode : " + response.status + " )")
          throw AutoPluginException("Error during create job")
        } else {
          //response.json.as[Job]
          (response.json \ "id").as[Int]
        }
      }
    Await.result(id, 10 seconds)
  }

  def updateJob(job: Job) {
    logger.debug("  >> Update Job ... ")
    val json = Json.toJson(job)(jobWrites)
    ws.url(jobSettings.urlApi + "/platform/" + jobSettings.platformId + "/job/" + jobSettings.jobId + "/version")
      .withAuth(jobSettings.login, jobSettings.password, WSAuthScheme.BASIC)
      .post(json).map { response =>
      if (response.status != 200) {
        logger.error("Error during update job(ErrorCode : " + response.status + " )")
        throw AutoPluginException("Error during the job update")
      }
    }
  }

  def checkJobExists(): Job = {

    logger.debug("Check Job {" + jobSettings.jobId + "} Exists ... ")

    val job = ws.url(jobSettings.urlApi + "/platform/" + jobSettings.platformId + "/job/" + jobSettings.jobId)
      .withAuth(jobSettings.login, jobSettings.password, WSAuthScheme.BASIC)
      .get()
      .map { response => response.json.as[Job] }

    job onFailure { case f =>
      logger.error("Error during check Job Exists {id:" + jobSettings.jobId + "} : " +
        f.printStackTrace)
      throw AutoPluginException("Error during existing job validation")
    }

    job onSuccess { case j =>
      if (jobSettings.jobName.equals(j.name) && jobSettings.jobCategory.equals(j.category)) {
        logger.info("Job {id:" + jobSettings.jobId +
          ", name:" + jobSettings.jobName +
          ", category:" + jobSettings.jobCategory +
          "} exists")
      }
      else {
        logger.error("Error, the job don't correspond : Requested : {id:" + jobSettings.jobId +
          ", name:" + jobSettings.jobName +
          ", category:" + jobSettings.jobCategory +
          "} - In platform : {id:" + j.id +
          ", name:" + j.name +
          ", category:" + j.category +
          "}")
        throw AutoPluginException("Error during existing job validation")
      }
    }

    Await.result(job, 10 seconds)
  }
}
