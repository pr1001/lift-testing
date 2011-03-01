package com.bubblefoundry.liftdebug.model

import net.liftweb.mapper.{MetaProtoExtendedSession, ProtoExtendedSession}
import net.liftweb.common.Box

class Session extends ProtoExtendedSession[Session] {
  def getSingleton = Session // what's the "meta" server
}

object Session extends Session with MetaProtoExtendedSession[Session] {
  type UserType = User

  override def dbTableName = "session" // define the DB table name
  override def CookieName = "session_id"

  def logUserIdIn(uid: String): Unit = User.logUserIdIn(uid)
  def recoverUserId: Box[String] = User.currentUserId
}