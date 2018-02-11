/*
 * =========================================================================================
 * Copyright © 2013-2017 the kamon project <http://kamon.io/>
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
 * =========================================================================================
 */

package kamon.statsd

import com.typesafe.config.{Config, ConfigFactory}
import kamon.Kamon
import org.scalatest.{Matchers, WordSpec}

class SimpleMetricKeyGeneratorSpec extends WordSpec with Matchers {

  val defaultConfiguration: Config = ConfigFactory.parseString(
    """
      |kamon {
      |  environment {
      |    service = "kamon"
      |    host = "auto"
      |  }
      |  statsd.simple-metric-key-generator {
      |    include-hostname = true
      |    metric-name-normalization-strategy = normalize
      |  }
      |}
    """.stripMargin
  ).withFallback(ConfigFactory.load())

  "the SimpleMetricKeyGenerator" should {

    "generate metric names that follow the application.host.entity.entity-name.metric-name pattern by default" in {
      Kamon.reconfigure(defaultConfiguration)
      val generator = new SimpleMetricKeyGenerator(defaultConfiguration)
      val host = generator.normalizer(Kamon.environment.host)

      generator.generateKey("actor", Map("metric-name-1" -> "/user/example", "metric-name-2" -> "processing-time")) should be(s"kamon.$host.actor._user_example.processing-time")
      generator.generateKey("trace", Map("metric-name-1" -> "POST: /kamon/example", "metric-name-2" -> "elapsed-time")) should be(s"kamon.$host.trace.POST-_kamon_example.elapsed-time")
    }

    "generate metric names without tags that follow the application.host.entity.entity-name.metric-name pattern by default" in {
      Kamon.reconfigure(defaultConfiguration)
      val generator = new SimpleMetricKeyGenerator(defaultConfiguration)
      val host = generator.normalizer(Kamon.environment.host)

      generator.generateKey("actor", Map.empty) should be(s"kamon.$host.actor")
    }

    "removes host name when attribute 'include-hostname' is set to false" in {
      Kamon.reconfigure(defaultConfiguration)
      val hostOverrideConfig = ConfigFactory.parseString("kamon.statsd.simple-metric-key-generator.include-hostname = false")
      val generator = new SimpleMetricKeyGenerator(hostOverrideConfig.withFallback(defaultConfiguration))

      generator.generateKey("actor", Map("metric-name-1" -> "/user/example", "metric-name-2" -> "processing-time")) should be("kamon.actor._user_example.processing-time")
      generator.generateKey("trace", Map("metric-name-1" -> "POST: /kamon/example", "metric-name-2" -> "elapsed-time")) should be("kamon.trace.POST-_kamon_example.elapsed-time")
    }

    "remove spaces, colons and replace '/' with '_' when the normalization strategy is 'normalize'" in {
      Kamon.reconfigure(defaultConfiguration)
      val hostOverrideConfig = ConfigFactory.parseString("kamon.statsd.simple-metric-key-generator.metric-name-normalization-strategy = normalize")
      val generator = new SimpleMetricKeyGenerator(hostOverrideConfig.withFallback(defaultConfiguration))
      val host = generator.normalizer(Kamon.environment.host)

      generator.generateKey("actor", Map("metric-name-1" -> "/user/example", "metric-name-2" -> "processing-time")) should be(s"kamon.$host.actor._user_example.processing-time")
      generator.generateKey("trace", Map("metric-name-1" -> "POST: /kamon/example", "metric-name-2" -> "elapsed-time")) should be(s"kamon.$host.trace.POST-_kamon_example.elapsed-time")
    }

    "percent-encode special characters in the group name and hostname when the normalization strategy is 'normalize'" in {
      Kamon.reconfigure(defaultConfiguration)
      val hostOverrideConfig = ConfigFactory.parseString("kamon.statsd.simple-metric-key-generator.metric-name-normalization-strategy = percent-encode")
      val generator = new SimpleMetricKeyGenerator(hostOverrideConfig.withFallback(defaultConfiguration))
      val host = generator.normalizer(Kamon.environment.host)

      generator.generateKey("actor", Map("metric-name-1" -> "/user/example", "metric-name-2" -> "processing-time")) should be(s"kamon.$host.actor.%2Fuser%2Fexample.processing-time")
      generator.generateKey("trace", Map("metric-name-1" -> "POST: /kamon/example", "metric-name-2" -> "elapsed-time")) should be(s"kamon.$host.trace.POST%3A%20%2Fkamon%2Fexample.elapsed-time")
    }
  }

}
