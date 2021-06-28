package helpers.utils


import scala.collection.mutable.ListBuffer

/**
 * This class convert a case class instance to Map or List.
 * This makes it easier to iterate over case class elements.
 *
 * just import this to where you use case classes
 */
object CaseConverter {
  implicit class CaseTo[C](caseInstance: C) {

    def toMap(): Map[String, Any] = {
      caseInstance.getClass.getDeclaredFields.foldLeft(Map.empty[String, Any]) {
        (accumulator, field) => {
          field.setAccessible(true)
          accumulator + (field.getName -> field.get(caseInstance))
        }
      }
    }

    def toList(): List[Any] = {
      // .getDeclaredFields method's last element include the caller class name which is not relevant to the case class.
      // To exclude the caller caller class name, use .init method.
      val fields = caseInstance.getClass.getDeclaredFields.init
      val result = fields.map { field =>
        field.setAccessible(true)
        field.get(caseInstance)
      }
      result.toList
    }
  }



}
