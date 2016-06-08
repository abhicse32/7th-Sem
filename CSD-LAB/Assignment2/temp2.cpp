#include <iostream>
#include <bitset>
#include <boost/dynamic_bitset.hpp>
using namespace std;

int main()
{

  int i=1;
  i= 1^1;
  cout << i <<endl;
  const boost::dynamic_bitset<> b0(2, 0ul);
  std::cout << "bits(0) = " << b0 << std::endl;

  const boost::dynamic_bitset<> b1(2, 1ul);
  std::cout << "bits(1) = " << b1 << std::endl;

  const boost::dynamic_bitset<> b2(2, 2ul);
  std::cout << "bits(2) = " << b2 << std::endl;

  const boost::dynamic_bitset<> b3(2, 3ul);
  std::cout << "bits(3) = " << b3 << std::endl;

  return 0;
}