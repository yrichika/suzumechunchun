package testhelpers.utils

import scala.annotation.tailrec
import scala.util.Random

object TestRandom {

  val alphaOnly = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz"

  def char() = {
    val index = Random.nextInt(alphaOnly.length)
    alphaOnly.charAt(index)
  }

  def boolean() = Random.nextBoolean()

  /**
   * First Letter should always be non-numeric to avoid unexpected results when testing.
   */
  def string(length: Int) = {
    val firstLetter = char()
    s"$firstLetter${Random.alphanumeric.take(length - 1).mkString}"
  }


  /**
   *
   * @param max this is **EXCLUSIVE**, meaning output max is actually (max - 1). Be careful.
   */
  def int(max: Int) = Random.nextInt(max)
  /**
   * @param min this is **INCLUSIVE**.
   * @param max this is **EXCLUSIVE**. Same with `int()` method above.
   */
  def intBetween(min: Int = 0, max: Int = 1001) = Random.between(min, max)

  /**
   *
   * @param max this value is **EXCLUSIVE**
   * @param excepts
   * @return
   */
  @tailrec
  def intExcept(max: Int, excepts: List[Int]): Int = {
    val candidate = Random.nextInt(max)
    if (!excepts.contains(candidate)) candidate
    else intExcept(max, excepts)
  }

  /**
   *
   * @param min this value is **INCLUSIVE**
   * @param max this value is **EXCLUSIVE**
   * @param excepts
   * @return
   */
  @tailrec
  def intBetweenExcept(min: Int, max: Int, excepts: List[Int]): Int = {
    val candidate = Random.between(min, max)
    if (!excepts.contains(candidate)) candidate
    else intBetweenExcept(max, max, excepts)
  }
}
