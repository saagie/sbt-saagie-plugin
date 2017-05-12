import sbtrelease.Git
import sbtrelease.ReleasePlugin.autoImport.ReleaseTransformations._

releaseIgnoreUntrackedFiles := true

lazy val switchToDevelop = ReleaseStep(action = st => {
  val extracted = Project.extract(st)
  val git = extracted.get(releaseVcs).get.asInstanceOf[Git]
  git.status
  git.cmd("checkout", "develop") ! st.log
  git.status
  git.cmd("merge", "master") ! st.log
  git.status
  st.log.info("Merged master")
  st
})

lazy val revertToMaster = ReleaseStep(action = st => {
  val extracted = Project.extract(st)
  val git = extracted.get(releaseVcs).get.asInstanceOf[Git]
  git.status
  git.cmd("push", "origin", "develop") ! st.log
  git.status
  git.cmd("checkout", "master") ! st.log
  st
})

lazy val gitStatus = ReleaseStep(action = st => {
  val extracted = Project.extract(st)
  val git = extracted.get(releaseVcs).get.asInstanceOf[Git]
  git.status
  st
})

releaseCommitMessage := s"Setting version to ${(version in ThisBuild).value} [ci skip]"

releaseProcess := Seq(
  checkSnapshotDependencies,
  inquireVersions,
  runTest,
  setReleaseVersion,
  tagRelease,
  commitReleaseVersion,
  //  ReleaseStep(action = Command.process("publishSigned", _)),
  //  ReleaseStep(action = Command.process("sonatypeReleaseAll", _)),
  switchToDevelop,
  setNextVersion,
  commitNextVersion,
  revertToMaster,
  pushChanges
)
