package tshy0931.com.github.weichain.database

import com.redis.{RedisClient, RedisClientPool}
import monix.eval.Task
import tshy0931.com.github.weichain.module.ConfigurationModule._

trait Redis {

  lazy val redis: RedisClientPool = new RedisClientPool(redisHost, redisPort)

  def exec[A](f: RedisClient => A): Task[A] = Task { redis withClient f }
}
