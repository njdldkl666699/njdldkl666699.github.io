import json


def extract_jmh_useful(json_obj: dict) -> dict:
    searchCount_rawData_0 = json_obj["secondaryMetrics"]["searchCount"]["rawData"][0]
    searchCount_score_new = sum(searchCount_rawData_0) / len(searchCount_rawData_0)

    return {
        "params": json_obj["params"],
        "primaryMetric": {
            "score": json_obj["primaryMetric"]["score"],
            "scoreUnit": json_obj["primaryMetric"]["scoreUnit"],
            "rawData": json_obj["primaryMetric"]["rawData"],
        },
        "gc.alloc.rate.norm": {
            "score": json_obj["secondaryMetrics"]["gc.alloc.rate.norm"]["score"],
            "scoreUnit": json_obj["secondaryMetrics"]["gc.alloc.rate.norm"]["scoreUnit"],
            "rawData": json_obj["secondaryMetrics"]["gc.alloc.rate.norm"]["rawData"],
        },
        "searchCount": {
            "score": searchCount_score_new,  # 每轮Iteration中Invocations的平均值
            "scoreUnit": "#/op",  # 单位为每次操作的调用次数
            "rawData": searchCount_rawData_0,
        },
    }


def extract_jmh_metadata(json_obj: dict) -> dict:
    return {
        "warmupIterations": json_obj["warmupIterations"],
        "warmupTime": json_obj["warmupTime"],
        "measurementIterations": json_obj["measurementIterations"],
        "measurementTime": json_obj["measurementTime"],
    }


def extract_jmh_readable(path: str) -> dict:
    with open(path, "r") as f:
        json_array = json.load(f)

        metadata = extract_jmh_metadata(json_array[0])
        results = [extract_jmh_useful(item) for item in json_array]
        return {
            "metadata": metadata,
            "results": results,
        }


if __name__ == "__main__":
    benchmark_algorithm = extract_jmh_readable("benchmarkAlgorithm.json")
    benchmark_algorithm_random = extract_jmh_readable("benchmarkAlgorithmRandom.json")

    # 保存到新的文件中
    with open("benchmarkAlgorithm_readable.json", "w") as f:
        json.dump(benchmark_algorithm, f, indent=2)
    with open("benchmarkAlgorithmRandom_readable.json", "w") as f:
        json.dump(benchmark_algorithm_random, f, indent=2)
