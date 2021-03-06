/** defines utilities to calculate the versioning values */
object Versioning {

  import java.time._
  import sbt._
  import sbt.Keys._

  private def zeroPadded(upto: Int) =
    (s: String) => (("0" * upto) ++ s.take(upto)) takeRight upto

  //currently allows "-SNAPSHOT" as a valid suffix for a release
  val releasePattern = raw"^(.+)-release-(\d+)(-.*)?".r

  /**
    * implement the logic for versioning explained in the build file
    * the tag parameter should have a format like:
    *
    *                  *--------*
    *                  |        | <-- this latter part is the result of `git describe`
    * <free>-release-1-3-d3a9863           and should be missing on the release commit
    *   ^            ^
    *   |            |
    *   |            *-- this is the version we care about
    *   |
    *   *- freeform (year-month would be the one used by sbt tagging)
    */
  def generate(major: Int, date: LocalDate, tag: String): String = {
    val week = zeroPadded(2)(date.get(temporal.ChronoField.ALIGNED_WEEK_OF_YEAR).toString)
    val year = date.getYear.toString.takeRight(2)
    val nextTagVersion = releasePattern.findAllIn(tag).group(2).toInt + 1
    val paddedTagVersion = zeroPadded(4)(String.valueOf(nextTagVersion))
    s"$major.$year$week.$paddedTagVersion"
  }

  /**
    * Reads the current version in the format created by the generate method
    * Then extracts the relevant tag counter, that is, the <n> in "major.yyww.<n>",
    * removes the leading zeroes and gives back the potential new release tag
    */
  lazy val prepareReleaseTagDef = Def.task {
    val currentVersion = version.value
    val tag = currentVersion.split('.').last.dropWhile(_ == '0')
    val date = LocalDate.now().formatted("%1$tY-%1$tB").toLowerCase
    s"$date-release-$tag"
  }

}
