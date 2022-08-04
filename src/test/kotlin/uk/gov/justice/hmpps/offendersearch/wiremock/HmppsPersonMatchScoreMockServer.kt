package uk.gov.justice.hmpps.offendersearch.wiremock

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.*
import org.junit.jupiter.api.extension.AfterAllCallback
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext

class HmppsPersonMatchScoreExtension : BeforeAllCallback, AfterAllCallback, BeforeEachCallback {
  companion object {
    @JvmField
    val hmppsPersonMatchScore = HmppsPersonMatchScoreMockServer()
  }

  override fun beforeAll(context: ExtensionContext) {
    hmppsPersonMatchScore.start()
  }

  override fun beforeEach(context: ExtensionContext) {
    hmppsPersonMatchScore.resetRequests()
  }

  override fun afterAll(context: ExtensionContext) {
    hmppsPersonMatchScore.stop()
  }
}

private const val RESPONSE = """{
    "dob_std_l": {
        "0": "2009-07-06"
    },
    "dob_std_r": {
        "0": "2009-07-06"
    },
    "forename1_std_l": {
        "0": "lily"
    },
    "forename1_std_r": {
        "0": "lily"
    },
    "forename2_std_l": {
        "0": null
    },
    "forename2_std_r": {
        "0": null
    },
    "forename3_std_l": {
        "0": null
    },
    "forename3_std_r": {
        "0": null
    },
    "forename4_std_l": {
        "0": null
    },
    "forename4_std_r": {
        "0": null
    },
    "forename5_std_l": {
        "0": null
    },
    "forename5_std_r": {
        "0": null
    },
    "gamma_dob_std": {
        "0": 5
    },
    "gamma_forename1_std": {
        "0": 3
    },
    "gamma_forename2_std": {
        "0": -1
    },
    "gamma_forename3_std": {
        "0": -1
    },
    "gamma_pnc_number_std": {
        "0": -1
    },
    "gamma_surname_std": {
        "0": 1
    },
    "match_probability": {
        "0": 0.9172587927
    },
    "pnc_number_std_l": {
        "0": "2001/0141640Y"
    },
    "pnc_number_std_r": {
        "0": null
    },
    "source_dataset_l": {
        "0": "COMMON_PLATFORM"
    },
    "source_dataset_r": {
        "0": "DELIUS"
    },
    "surname_std_l": {
        "0": "robinson"
    },
    "surname_std_r": {
        "0": "robibnson"
    },
    "unique_id_l": {
        "0": "nan"
    },
    "unique_id_r": {
        "0": "nan"
    }
}"""

private const val REQUEST = """{
  "unique_id": {
    "0": "1111",
    "1": "4444"
  },
  "first_name": {
    "0": "ann",
    "1": "anne"
  },
  "surname": {
    "0": "grammsci",
    "1": "gramsci"
  },
  "dob": {
    "0": "1988-01-07",
    "1": "1988-01-06"
  },
  "pnc_number": {
    "0": "3018/0123456X",
    "1": "2018/0123456X"
  },
  "source_dataset": {
    "0": "COMMON_PLATFORM",
    "1": "DELIUS"
  }
}"""

class HmppsPersonMatchScoreMockServer : WireMockServer(WIREMOCK_PORT) {
  companion object {
    private const val WIREMOCK_PORT = 9091
  }

  fun stubPersonMatchScore() {
    stubFor(
      post("/match")
        .withRequestBody(equalToJson(REQUEST))
        .willReturn(
        aResponse()
          .withHeader("Content-Type", "application/json")
          .withBody(RESPONSE)
          .withStatus(200)
      )
    )
  }

  fun stubPersonMatchScoreError() {
    stubFor(
      post("/match")
        .withRequestBody(equalToJson(REQUEST))
        .willReturn(
          aResponse()
            .withBody("ERROR")
            .withStatus(500)
        )
    )
  }
}
