package com.bubblefoundry.liftdebug.model

import org.scalatest.fixture.FixtureFlatSpec
import org.scalatest.matchers.ShouldMatchers

import net.liftweb.mapper._
import net.liftweb.common.{Empty, Full, Logger}
import net.liftweb.http.{LiftSession, S}
import net.liftweb.util.StringHelpers

object InMemoryDB {
  val vendor = new StandardDBVendor("org.h2.Driver", "jdbc:h2:mem:lift;DB_CLOSE_DELAY=-1", Empty, Empty)
  Logger.setup = Full(net.liftweb.util.LoggingAutoConfigurer())
  Logger.setup.foreach { _.apply() }
  
  def init[T <: MetaMapper[_]](models: T*) {
    DB.defineConnectionManager(DefaultConnectionIdentifier, vendor)
    Schemifier.destroyTables_!!(Schemifier.infoF _, models:_*)
    Schemifier.schemify(true, Schemifier.infoF _, models:_*)
  }

  def shutdown {
    // TODO?
  }
}

class SessionSpec extends FixtureFlatSpec with ShouldMatchers {

  type FixtureParam = User

  val session: LiftSession = new LiftSession("", StringHelpers.randomString(20), Empty)

  override def withFixture(test: OneArgTest) {
    // set up your db here
    InMemoryDB.init(User, Session)
    try {
      // Initialize session state if it is not already
      S.initIfUninitted(session) {
        // Create the user
        DB.use(DefaultConnectionIdentifier){ connection =>
          val johnDoe = User.create.firstName("John").lastName("Doe").saveMe
          test(johnDoe) // Invoke the test function
        }
      }
    }
    finally {
      // tear down your db here
      InMemoryDB.shutdown
    }
  }
  
  "Session" should "login a user" in { johnDoe =>
    Session.logUserIdIn(johnDoe.userIdAsString)
    User.currentUser should equal (Full(johnDoe))
    println(Session.findAll)
  }
  
  it should "create the Session database entry on login" in { johnDoe =>
    Session.logUserIdIn(johnDoe.userIdAsString)
    val session = Session.find(By(Session.userId, johnDoe.userIdAsString))
    session should not equal (Empty)
  }

  it should "create the session_id cookie on login" in { johnDoe =>
    Session.logUserIdIn(johnDoe.userIdAsString)
    println(Session.findAll)
    val session = Session.find(By(Session.userId, johnDoe.userIdAsString))
    S.cookieValue(Session.CookieName) should not equal (Empty)
  }
  
  it should "have a session_id cookie value that matches the database entry" in { johnDoe =>
    Session.logUserIdIn(johnDoe.userIdAsString)
    val session = Session.find(By(Session.userId, johnDoe.userIdAsString))
    S.cookieValue(Session.CookieName) should not equal (Empty)
    session should not equal (Empty)
    S.cookieValue(Session.CookieName) should equal (session.map(_.id))
  }
  
  it should "destroy the Session database entry on logout" in { johnDoe =>
    Session.logUserIdIn(johnDoe.userIdAsString)
    val session = Session.find(By(Session.userId, johnDoe.userIdAsString))
    session should not equal (Empty)
    User.logoutCurrentUser
    val session2 = Session.find(By(Session.userId, johnDoe.userIdAsString))
    session2 should equal (Empty)
  }
  
  it should "destroy the session_id cookie on logout" in { johnDoe =>
    Session.logUserIdIn(johnDoe.userIdAsString)
    S.cookieValue(Session.CookieName) should not equal (Empty)
    User.logoutCurrentUser
    S.cookieValue(Session.CookieName) should equal (Empty)
  }

  // TODO
  it should "destroy the Session database entry on expiration" in { johnDoe =>
    pending
    Session.logUserIdIn(johnDoe.userIdAsString)
    val session = Session.find(By(Session.userId, johnDoe.userIdAsString))
    session should not equal (Empty)
    User.logoutCurrentUser
    val session2 = Session.find(By(Session.userId, johnDoe.userIdAsString))
    session2 should equal (Empty)
  }
  
  // TODO
  it should "destroy the session_id cookie on expiration" in { johnDoe =>
    pending
    Session.logUserIdIn(johnDoe.userIdAsString)
    S.cookieValue(Session.CookieName) should not equal (Empty)
    User.logoutCurrentUser
    S.cookieValue(Session.CookieName) should equal (Empty)
  }
}