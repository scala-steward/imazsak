package hu.ksisu.imazsak

import akka.actor.ActorSystem
import akka.stream.Materializer
import cats.MonadError
import hu.ksisu.imazsak.core._
import hu.ksisu.imazsak.core.config.{ServerConfig, ServerConfigImpl}
import hu.ksisu.imazsak.core.dao._
import hu.ksisu.imazsak.core.healthcheck.{HealthCheckService, HealthCheckServiceImpl}
import hu.ksisu.imazsak.core.impl._
import hu.ksisu.imazsak.group.{GroupService, GroupServiceImpl}
import hu.ksisu.imazsak.me.{MeService, MeServiceImpl}
import hu.ksisu.imazsak.util._
import org.slf4j.Logger
import reactivemongo.api.MongoDriver

import scala.concurrent.{ExecutionContext, Future}

trait Services[F[_]] {
  implicit val configService: ServerConfig[F]
  implicit val healthCheckService: HealthCheckService[F]
  implicit val databaseService: MongoDatabaseService[F]
  implicit val idGenerator: IdGenerator[F]
  implicit val dateTimeService: DateTimeUtil[F]
  implicit val tracerService: TracerService[F]
  implicit val amqpService: AmqpService[F]
  implicit val jwtService: JwtService[F]
  implicit val userDao: UserDao[F]
  implicit val meService: MeService[F]
  implicit val groupDao: GroupDao[F]
  implicit val groupService: GroupService[F]

  def init()(implicit logger: Logger, ev: MonadError[F, Throwable]): F[Unit] = {
    import Initable._
    import cats.syntax.flatMap._
    import cats.syntax.functor._

    logger.info("Init services")
    for {
      _ <- initialize(configService, "config")
      _ <- initialize(tracerService, "tracer")
      _ <- initialize(databaseService, "database")
      _ <- initialize(amqpService, "ampq")
    } yield ()
  }
}

class RealServices(implicit ec: ExecutionContext, actorSystem: ActorSystem, materializer: Materializer)
    extends Services[Future] {

  import cats.instances.future._

  implicit lazy val configService: ServerConfig[Future] = new ServerConfigImpl[Future]
  import configService._

  implicit lazy val healthCheckService: HealthCheckService[Future] = new HealthCheckServiceImpl[Future]
  implicit lazy val mongoDriver: MongoDriver                       = new MongoDriver()
  implicit lazy val databaseService: MongoDatabaseService[Future]  = new MongoDatabaseServiceImpl()
  implicit lazy val httpWrapper: HttpWrapper[Future]               = new AkkaHttpWrapper()
  implicit lazy val idGenerator: IdGenerator[Future]               = new IdGeneratorImpl[Future]
  implicit lazy val dateTimeService: DateTimeUtil[Future]          = new DateTimeUtilImpl[Future]
  implicit lazy val tracerService: TracerService[Future]           = new TracerService[Future]()
  implicit lazy val amqpService: AmqpService[Future]               = new AmqpServiceImpl[Future]()
  implicit lazy val jwtService: JwtServiceImpl[Future]             = new JwtServiceImpl[Future]()
  implicit lazy val userDao: UserDao[Future]                       = new UserDaoImpl()
  implicit lazy val meService: MeService[Future]                   = new MeServiceImpl[Future]()
  implicit lazy val groupDao: GroupDao[Future]                     = new GroupDaoImpl()
  implicit lazy val groupService: GroupService[Future]             = new GroupServiceImpl()
}
