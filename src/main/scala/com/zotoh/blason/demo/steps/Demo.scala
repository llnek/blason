/*??
 * COPYRIGHT (C) 2012 CHERIMOIA LLC. ALL RIGHTS RESERVED.
 *
 * THIS IS FREE SOFTWARE; YOU CAN REDISTRIBUTE IT AND/OR
 * MODIFY IT UNDER THE TERMS OF THE APACHE LICENSE,
 * VERSION 2.0 (THE "LICENSE").
 *
 * THIS LIBRARY IS DISTRIBUTED IN THE HOPE THAT IT WILL BE USEFUL,
 * BUT WITHOUT ANY WARRANTY; WITHOUT EVEN THE IMPLIED WARRANTY OF
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE.
 *
 * SEE THE LICENSE FOR THE SPECIFIC LANGUAGE GOVERNING PERMISSIONS
 * AND LIMITATIONS UNDER THE LICENSE.
 *
 * You should have received a copy of the Apache License
 * along with this distribution; if not, you may obtain a copy of the
 * License at
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 ??*/

package demo.steps

import com.zotoh.blason.kernel._
import com.zotoh.blason.wflow._
import com.zotoh.blason.io._

/**
 * @author kenl
 */
class DemoMain(c:Container) {
  def start() {
    println("Demo a set of workflow control features..." )
  }
  def stop() {
  }
  def dispose() {
  }
}


/**
 * What this example demostrates is a webservice which takes in some user info, authenticate the
 * user, then perform some EC2 operations such as granting permission to access an AMI, and
 * permission to access/snapshot a given volume.  When all is done, a reply will be sent back
 * to the user.
 *
 * This flow showcases the use of conditional activities such a Switch() &amp; If().  Shows how to loop using
 * While(), and how to use Split &amp; Join.
 *
 * @author kenl
 *
 */
class Demo(job:Job) extends Pipeline(job) {
  import Auth._

  override def onEnd() {
    // we override this just to show/indicate that we are done.
    println("Finally, workflow is done.!")
  }

  // step1. choose a method to authenticate the user
  // here, we'll use a switch() to pick which method
  private val AuthUser = new Switch().
          withChoice("facebook", getAuthMtd("facebook")).
          withChoice("google+", getAuthMtd("google+")).
          withChoice("openid", getAuthMtd("openid")).
          withDef( getAuthMtd("db")).
          withExpr(new SwitchChoiceExpr() {
            def eval(j:Job) = {
              // hard code to use facebook in this example, but you
              // could check some data from the job, such as URI/Query params
              // and decide on which mth-value to switch() on.
              println("Step(1): Choose an authentication method.")
              "facebook"
            }
          })

  // step2.
  private val get_profile = new Work {
    def eval(j:Job,arg:Any*) {
      println("Step(2): Get user profile\n-> user is superuser.\n")
    }
  }

  private val GetProfile = new PTask(get_profile)

  // step3. we are going to dummy up a retry of 2 times to simulate network/operation
  // issues encountered with EC2 while trying to grant permission.
  // so here , we are using a while() to do that.
  private val perm_ami = new Work {
    def eval(j:Job,arg:Any*) {
      j.getData("ami_count") match {
        case Some(n:Int) if (n == 2) =>
          println("Step(3): Granted permission for user to launch this ami(id).\n")
        case Some(n:Int) =>
          println("Step(3): Failed to contact ami- server, will retry again... ("+n+") ")
        case _ =>
      }
    }
  }

  private val prov_ami = new While withBody new PTask(perm_ami) withExpr(
      // the while (test-condition)
      new BoolExpr() {
        def eval(j:Job) = {
          var c=0
          j.getData("ami_count") match {
            case Some(n:Int) => 
              // we are going to dummy up so it will retry 2 times
              c= n+1
            case _ =>
          }
          j.setData("ami_count", c)
          c < 3
        }
      })

  // step3'. we are going to dummy up a retry of 2 times to simulate network/operation
  // issues encountered with EC2 while trying to grant volume permission.
  // so here , we are using a while() to do that.
  private val perm_vol = new Work {
    def eval(j:Job,arg:Any*) {
      j.getData("vol_count") match {
        case Some(n:Int) if (n==2) =>
          println("Step(3'): Granted permission for user to access/snapshot this volume(id).\n")
        case Some(n:Int) =>
          println("Step(3'): Failed to contact vol- server, will retry again... ("+n+") ")
        case _ =>
      }
    }
  }

  private val prov_vol = new While withBody new PTask(perm_vol) withExpr(
    // the while (test-condition)
    new BoolExpr() {
      def eval(j:Job) = {
        var c=0
        j.getData( "vol_count") match {
          case Some(n:Int) =>
            // we are going to dummy up so it will retry 2 times
             c = n +1
          case _ =>
        }
        j.setData("vol_count", c)
        c < 3
      }
    })

  // step4. pretend to write stuff to db. again, we are going to dummy up the case
  // where the db write fails a couple of times.
  // so again , we are using a while() to do that.
  private val write_db = new Work {
    def eval(j:Job,arg:Any*)  {
      j.getData("wdb_count") match {
        case Some(n:Int) if (n==2) =>
          println("Step(4): Wrote stuff to database successfully.\n")
        case Some(n:Int) =>
          println("Step(4): Failed to contact db- server, will retry again... ("+n+") ")
        case _ =>
      }
    }
  }

  private val save_sdb = new While withBody new PTask(write_db) withExpr(
    // the while (test-condition)
    new BoolExpr() {
      def eval(j:Job) = {
        var c=0
        j.getData("wdb_count") match {
          case Some(n:Int) =>
            // we are going to dummy up so it will retry 2 times
             c= n+1
          case _ =>
        }
        j.setData("wdb_count", c)
        c < 3
      }
    })

  // this is the step where it will do the provisioning of the AMI and the EBS volume
  // in parallel.  To do that, we use a split-we want to fork off both tasks in parallel.  Since
  // we don't want to continue until both provisioning tasks are done. we use a AndJoin to hold/freeze
  // the workflow.
  private val Provision = new Split().addSplit(prov_ami).addSplit(prov_vol).
          withJoin(new And() withBody(save_sdb))

  // this is the final step, after all the work are done, reply back to the caller.
  // like, returning a 200-OK.
  private val reply_user = new Work {
    def eval(j:Job,arg:Any*) {
      println("Step(5): We'd probably return a 200 OK back to caller here.\n")
    }
  }

  private val ReplyUser = new PTask(reply_user)

  private val error_user = new Work {
    def eval(j:Job,arg:Any*) {
      println("Step(5): We'd probably return a 200 OK but with errors.\n")
    }
  }

  private val ErrorUser = new PTask(error_user)


  // do a final test to see what sort of response should we send back to the user.
  private val FinalTest = new If().
    withThen(ReplyUser).
    withElse(ErrorUser).
    withExpr(new BoolExpr() {
      def eval(j:Job ) = {
        // we hard code that all things are well.
        true
      }
    })



  // returning the 1st step of the workflow.
  def onStart() = {

    // so, the workflow is a small (4 step) workflow, with the 3rd step (Provision) being
    // a split, which forks off more steps in parallel.

    new Group(AuthUser) chain(GetProfile) chain(Provision) chain(FinalTest)
  }

}

