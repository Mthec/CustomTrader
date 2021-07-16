rootProject.name = "CustomTrader"
include(":BMLBuilder", ":CreatureCustomiser", ":PlaceNpc", ":TradeLibrary", ":WurmTestingHelper")
project(":BMLBuilder").projectDir = file("../BMLBuilder")
project(":CreatureCustomiser").projectDir = file("../CreatureCustomiser")
project(":PlaceNpc").projectDir = file("../PlaceNpc")
project(":TradeLibrary").projectDir = file("../TradeLibrary")
project(":WurmTestingHelper").projectDir = file("../WurmTestingHelper")