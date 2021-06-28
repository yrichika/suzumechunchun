package database.factories

import testhelpers.UnitTestCase


class ModelFactorySpec extends UnitTestCase {


  case class Dummy(name: String, age: Int)

  trait DummyFactory extends ModelFactory[Dummy] {
    override def define = {
      Dummy(name, age)
    }
    def name = "dummyName"
    def age = 1
  }

  "factory trait" should {
    "create collection instance of a case class" in {
      val result = new DummyFactory{}.create()
      result.map (model => {
        model.name should be ("dummyName")
        model.age should be (1)
      })
    }
  }


  it should {
    "create specified number of case class instances" in {
      val result = new DummyFactory {}.create(3)
      result.size should be (3)
    }
  }


  "factory attributes" should {
    "be able to be overriden at instantiation" in {
      val result = new DummyFactory {
        override def name = "newName"
      } .create()
      result.map (model => model.name should be ("newName"))
    }
  }


}

