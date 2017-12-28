package tech.cryptonomic.conseil.routes

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import com.typesafe.scalalogging.LazyLogging
import tech.cryptonomic.conseil.tezos.{ApiOperations, TezosNodeInterface}
import tech.cryptonomic.conseil.util.{DatabaseUtil, JsonUtil}

import scala.util.{Failure, Success}

/**
  * Tezos-specific routes.
  */
object Tezos extends LazyLogging {

  val dbHandle = DatabaseUtil.db
  //val tezosDB = ApiOperations

  val route: Route = pathPrefix(Segment) { network =>
    pathPrefix("blocks") {
      get {
        pathEnd {
          complete(TezosNodeInterface.runQuery(network, "blocks"))
        } ~ path("head") {
          ApiOperations.fetchLatestBlock match {
            case Success(block) => complete(JsonUtil.toJson(block))
            case Failure(e) => failWith(e)
          }
        } ~ path(Segment) { blockId =>
          complete(TezosNodeInterface.runQuery(network, s"blocks/$blockId"))
        }
      }
    } ~ pathPrefix("accounts") {
      get {
        pathEnd {
          complete(TezosNodeInterface.runQuery(network, "blocks/head/proto/context/contracts"))
        } ~ path(Segment) { accountId =>
          complete(TezosNodeInterface.runQuery(network, s"blocks/head/proto/context/contracts/$accountId"))
        }
      } ~ pathPrefix("operations") {
        get {
          pathEnd {
            complete(TezosNodeInterface.runQuery(network, "blocks/head/proto/operations"))
          }
        }
      }
    }
  }
}
