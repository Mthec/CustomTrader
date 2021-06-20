rootProject.name = "CustomTrader"
include(":BMLBuilder", ":CreatureCustomiser", ":PlaceNpc", ":WurmTestingHelper")
project(":BMLBuilder").projectDir = file("../BMLBuilder")
project(":CreatureCustomiser").projectDir = file("../CreatureCustomiser")
project(":PlaceNpc").projectDir = file("../PlaceNpc")
project(":WurmTestingHelper").projectDir = file("../WurmTestingHelper")