package database.factories

trait ModelFactory[T] {
  def create(howMany: Int = 1): Seq[T] = {
    for (i <- 0 until howMany) yield {
      define
    }
  }

  def define: T
}
