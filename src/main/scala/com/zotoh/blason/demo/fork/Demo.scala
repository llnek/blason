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

package demo.fork

import com.zotoh.blason.kernel._
import com.zotoh.blason.wflow._
import com.zotoh.blason.io._

/**
 * @author kenl
 */
class DemoMain(c:Container) {
  def start() {    
    println("Demo fork(split)/join of tasks..." )
  }
  def stop() {    
  }
  def dispose() {    
  }
}

/**
 * @author kenl
 *
    parent(s1) --> split&nowait
                   |-------------> child(s1)----> split&wait --> grand-child
                   |                              |                    |
                   |                              |<-------------------+
                   |                              |---> child(s2) -------> end
                   |
                   |-------> parent(s2)----> end
 */
class Demo(job:Job) extends Pipeline(job) {

  val parent1= new Work {
      def eval(job:Job,arg:Any*) {
          println("I am the *Parent*")
          println("I am programmed to fork off a parallel child process, and continue my business.")
      }
  }

  val parent2= new Work {
      def eval(job:Job,arg:Any*) {
        def fib(n:Int):Int = {
            if (n <3) 1 else { fib(n-2) + fib(n-1) }
        }
        println("*Parent*: after fork, continue to calculate fib(6)...")
        val b=new StringBuilder
        b.append("*Parent*: ")
        for (i <- 1 to 6) {
            b.append( fib(i) + " ")
        }
        println(b.toString )
        println("*Parent*: done.")
      }
  }

  val gchild= new Work {
      def eval(job:Job,arg:Any*) {
        println("*Child->child*: taking some time to do this task... ( ~ 6secs)")
        for (i <- 1 to 6) {
          Thread.sleep(1000)
          print("...")
        }
        println("")
        println("*Child->child*: returning result back to *Child*.")
        job.setData("result",  job.getData("rhs").get.asInstanceOf[Int] * 
            job.getData("lhs").get.asInstanceOf[Int]
        )
        println("*Child->child*: done.")
      }
  }

  val child=new Work {
      def eval(job:Job,arg:Any*) {
          println("*Child*: will create my own child (blocking)")
          job.setData("rhs", 60)
          job.setData("lhs", 5)
          val p2= new PTask withWork new Work {
              def eval(job:Job,arg:Any*) {
                  println("*Child*: the result for (5 * 60) according to my own child is = "  +
                          job.getData("result").get)
                  println("*Child*: done.")
              }
          }
                  // split & wait
          val a= new Split( new And withBody(p2)) addSplit new PTask(gchild)
          setResult(a)
      }
    }

    // split but no wait
    // parent continues;
    override def onStart() = new PTask(parent1) chain(
        new Split() addSplit( new PTask(child) )) chain( new PTask(parent2) )

}

