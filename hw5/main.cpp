#include <iostream>
#include <fstream>
#include <vector>
#include <chrono>
#include <omp.h>

const float EPS = 1.0e+05;
int num_threads;

std::vector<float> read_file(const std::string & file_name) {
    std::ifstream fin;
    fin.open(file_name);
    if (!fin.is_open()) {
        std::cerr << "Cannot open file \"" << file_name << "\"";
    }
    int size;
    fin >> size;
    std::vector<float> array(size);
    for (float & a : array) {
        fin >> a;
    }
    fin.close();
    return array;
}

void write_file(const std::vector<float> & array, const std::string & file_name) {
    std::ofstream fout;
    fout.open(file_name);
    if (!fout.is_open()) {
        std::cerr << "Cannot open file \"" << file_name << "\"";
    }
    fout << array.size() << "\n";
    for (float a : array) {
        fout << a << " ";
    }
    fout.close();
}

std::vector<float> not_parallel(const std::vector<float> & array) {
    std::vector<float> prefix(array.size());
    std::chrono::system_clock::time_point start_time = std::chrono::system_clock::now();
    prefix[0] = array[0];
    for (int i = 1; i < array.size(); i++) {
        prefix[i] = array[i] + prefix[i - 1];
    }
    std::chrono::system_clock::time_point end_time = std::chrono::system_clock::now();
    std::chrono::duration<float> time = end_time - start_time;
    printf("\nNot parallel time (%i thread(s)): %f ms\n", num_threads, time.count());
    return prefix;
}


int get_log2(int n) {
    int res = 0;
    while (n > (1 << res)) {
        res++;
    }
    return res;
}

std::vector<float> parallel(const std::vector<float> & array) {
    int n = array.size();
    int logn = get_log2(n);
    std::vector<float> temp (n * (logn + 1));
    temp[0] = 0;
#pragma omp parallel for default(none) shared(temp, array, n)
    for (int i = 1; i < n; i++) {
        temp[i] = array[i - 1];
    }

    std::chrono::system_clock::time_point start_time = std::chrono::system_clock::now();
    for (int d = 1; d < logn + 1; d++) {
#pragma omp parallel for default(none) shared(temp, n, d)
        for (int k = 0; k < n; k++) {
            if (k >= (1 << (d - 1))) {
                temp[d * n + k] = temp[(d - 1) * n + k] + temp[(d - 1) * n + k - (1 << (d - 1))];
            } else {
                temp[d * n + k] = temp[(d - 1) * n + k];
            }
        }
    }
    std::chrono::system_clock::time_point end_time = std::chrono::system_clock::now();
    std::chrono::duration<float> time = end_time - start_time;
    printf("\nParallel time (%i thread(s)): %f ms\n", num_threads, time.count());

    std::vector<float> prefix(array.size());
#pragma omp parallel for default(none) shared(temp, array, prefix, logn, n)
    for (int i = 0; i < n; ++i) {
        prefix[i] = temp[n * logn + i] + array[i];
    }
    return prefix;
}

int main(int argc, char *argv[]) {
    if (argc < 3) {
        std::cerr << "Run this file with args <num_thread> <input_file> [<output_file>]";
        return 1;
    }
    try {
        num_threads = std::stoi(argv[1]);
    } catch (int i) {
        std::cerr << "Cannot cast \"" << argv[1] << "\" to int";
        return 1;
    }

    if (num_threads < 0) {
        std::cerr << "num of thread must be >= 0";
        return 1;
    }
    omp_set_dynamic(0);
    omp_set_num_threads(num_threads);

    std::string input_file_name = argv[2];
    std::vector<float> array = read_file(input_file_name), prefix_np, prefix_p;
    prefix_np = not_parallel(array);
    prefix_p = parallel(array);
    for (int i = 0; i < array.size(); ++i) {
        if (std::abs(prefix_p[i] - prefix_np[i]) > EPS) {
            std::cerr << "Algorithm error:\n" << "expected: " << prefix_np[i] << "\n" << "actual: " << prefix_p[i];
            return 1;
        }
    }

    if (argc == 4) {
        std::string output_file_name = argv[3];
        write_file(prefix_p, output_file_name);
    } else {
        std::cout << prefix_p.size() << std::endl;
        for (float a : prefix_p) {
            std::cout << a << " ";
        }
    }
    return 0;
}


