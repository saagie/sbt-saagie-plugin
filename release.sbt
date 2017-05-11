import sbtrelease.Git
import sbtrelease.ReleasePlugin.autoImport.ReleaseTransformations._

lazy val switchToDevelop = ReleaseStep(action = st => {
  val extracted = Project.extract(st)
  val git = extracted.get(releaseVcs).get.asInstanceOf[Git]
  git.cmd("checkout", "develop") ! st.log
  git.cmd("merge", "master") ! st.log
  st.log.info("Merged master")
  st
})

lazy val revertToMaster = ReleaseStep(action = st => {
  val extracted = Project.extract(st)
  val git = extracted.get(releaseVcs).get.asInstanceOf[Git]
  git.cmd("push", "origin", "develop") ! st.log
  git.cmd("checkout", "master") ! st.log
  st
})

releaseProcess := Seq(
  checkSnapshotDependencies,
  inquireVersions,
  runTest,
  tagRelease,
  ReleaseStep(action = Command.process("publishSigned", _)),
  ReleaseStep(action = Command.process("sonatypeReleaseAll", _)),
  switchToDevelop,
  setNextVersion,
  commitNextVersion,
  revertToMaster,
  pushChanges
)
