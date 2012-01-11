package models

import play.api._
import util.{AuthenticationAccessor, AuthenticationProvider}

case class UserException(message: String) extends Exception(message)

abstract class User(val username: String, val password: String) {
  def authenticate(username: String, password: String) = User.authenticate(username, password)
  def isAuthenticated(): Boolean
  def id(): Int
  def roles(): Seq[String]
  def hasRole(name: String): Boolean = roles().contains(name)
  def getRole[T](name: String): Option[String] = hasRole(name) match {
    case true => Some(name)
    case false => None
  }
  def toMap(): Map[String,String] = Map(
    User.ID -> id().toString(),
    User.USERNAME -> username,
    User.IS_AUTHENTICATED -> isAuthenticated().toString,
    User.ROLES -> roles().mkString(",")
    )
}
case class UserImpl(_username: String, _password: String, _roles: Seq[String], _id: Int, _authenticated: Boolean)
  extends User(_username, _password)
{
  override def id() = _id
  override def roles() = _roles
  override def isAuthenticated() = _authenticated
}

object User {
  private val ID = "id"
  private val IS_AUTHENTICATED = "authenticated"
  private val USERNAME = "username"
  private val ROLES = "roles"

  def authenticate(username: String, password: String, provider: Option[AuthenticationProvider] = None) = {
    val p = provider match {
      case None => getProviderFromFramework()
      case Some(p) => p
    }
    p.authenticate(username, password)
  }

  private def getProviderFromFramework(): AuthenticationProvider = {
    Play.maybeApplication.map { app =>
      app.global match {
        case a: AuthenticationAccessor => a.getAuthentication()
        case _ => throw UserException("No authentication handler available")
      }
    }.getOrElse(throw UserException("Not in application"))
  }
  
  def fromMap(map: Map[String,String]): Option[User] = {
    val user = Map(
      USERNAME -> map.get(USERNAME),
      ID -> map.get(ID),
      ROLES    -> map.get(ROLES),
      IS_AUTHENTICATED -> map.get(IS_AUTHENTICATED)
    )
    val isInvalid = user.find { case(k,v) => !v.isDefined }.isDefined
    if ( isInvalid ) {
      None
    } else {
      val username = user(USERNAME).get
      val password = "*"
      val rles = user(ROLES).get.split(",").toSeq
      val is_auth = user(IS_AUTHENTICATED).get.equals("true")
      val _id = user(ID).get.toInt
      Some(new User(username, password) {
        override def isAuthenticated() = is_auth
        override def roles() = rles
        override def id() = _id
      })
    }
  }

  def toMap(user: Option[User]) = user.map { u =>
    u.toMap
  }.getOrElse(Map.empty[String,String])

}
