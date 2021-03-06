package hu.ksisu.imazsak

import cats.MonadError
import hu.ksisu.imazsak.core.config.ServerConfig
import org.slf4j.Logger

trait Initable[F[_]] {
  def init: F[Unit]
}

object Initable {
  import cats.syntax.applicative._
  import cats.syntax.applicativeError._

  def initialize[F[_]: MonadError[*[_], Throwable]](initable: => Initable[F], name: String)(implicit
      logger: Logger
  ): F[Unit] = {
    logger.debug(s"Init $name")
    initable.init.onError { case ex =>
      logger.error(s"Init $name failed", ex).pure[F]
    }
  }

  def initializeIfEnabled[F[_]: MonadError[*[_], Throwable]](
      initable: => Initable[F],
      name: String
  )(implicit conf: ServerConfig[F], logger: Logger): F[Unit] = {
    if (conf.getEnabledModules.contains(name)) initialize(initable, name)
    else ().pure[F]
  }
}
