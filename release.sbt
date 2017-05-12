import sbtrelease.Git
import sbtrelease.ReleasePlugin.autoImport.ReleaseTransformations._

releaseIgnoreUntrackedFiles := true

lazy val switchToDevelop = ReleaseStep(action = st => {
  val extracted = Project.extract(st)
  val git = extracted.get(releaseVcs).get.asInstanceOf[Git]
  git.status ! st.log
  git.cmd("checkout", "develop") ! st.log
  git.status ! st.log
  git.cmd("merge", "master") ! st.log
  git.status ! st.log
  st.log.info("Merged master")
  st
})

lazy val revertToMaster = ReleaseStep(action = st => {
  val extracted = Project.extract(st)
  val git = extracted.get(releaseVcs).get.asInstanceOf[Git]
  git.status ! st.log
  git.cmd("push", "origin", "develop") ! st.log
  git.status ! st.log
  git.cmd("checkout", "master") ! st.log
  st
})

lazy val gitStatus = ReleaseStep(action = st => {
  val extracted = Project.extract(st)
  val git = extracted.get(releaseVcs).get.asInstanceOf[Git]
  git.status ! st.log
  st
})

releaseCommitMessage := s"Setting version to ${(version in ThisBuild).value} [ci skip]"

releaseProcess := Seq(
  checkSnapshotDependencies,
  inquireVersions,
  runTest,
  gitStatus,
  setReleaseVersion,
  gitStatus,
  tagRelease,
  gitStatus,
  commitReleaseVersion,
  //  ReleaseStep(action = Command.process("publishSigned", _)),
  //  ReleaseStep(action = Command.process("sonatypeReleaseAll", _)),
  switchToDevelop,
  setNextVersion,
  commitNextVersion,
  revertToMaster,
  pushChanges
)
