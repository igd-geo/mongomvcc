// This file is part of MongoMVCC.
//
// Copyright (c) 2012 Fraunhofer IGD
//
// MongoMVCC is free software: you can redistribute it and/or modify
// it under the terms of the GNU Lesser General Public License as
// published by the Free Software Foundation, either version 3 of the
// License, or (at your option) any later version.
//
// MongoMVCC is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
// GNU Lesser General Public License for more details.
//
// You should have received a copy of the GNU Lesser General Public
// License along with MongoMVCC. If not, see <http://www.gnu.org/licenses/>.

package de.fhg.igd.mongomvcc.helper;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Provides an array of prime numbers optimized for hash tables
 * @author Michel Kraemer
 */
public final class Primes {
	private static final int[] PRIMES = {
		7, 11, 13, 17, 19, 23, 29, 31, 37, 41,
		43, 47, 53, 59, 61, 67, 71, 73, 79, 83,
		89, 97, 101, 107, 113, 127, 131, 139, 149, 157,
		167, 179, 191, 199, 211, 223, 239, 251, 269, 281,
		307, 331, 347, 367, 389, 409, 431, 457, 487, 521,
		547, 577, 607, 641, 677, 719, 757, 809, 853, 907,
		953, 1009, 1063, 1123, 1187, 1259, 1327, 1399, 1481, 1567,
		1657, 1747, 1847, 1949, 2063, 2179, 2297, 2423, 2557, 2699,
		2851, 3011, 3181, 3359, 3541, 3739, 3943, 4159, 4391, 4637,
		4889, 5167, 5449, 5749, 6067, 6421, 6763, 7151, 7537, 7951,
		8389, 8861, 9343, 9857, 10399, 10973, 11579, 12227, 12893, 13613,
		14369, 15161, 15991, 16871, 17807, 18787, 19819, 20921, 22067, 23279,
		24571, 25919, 27361, 28859, 30449, 32141, 33911, 35771, 37747, 39821,
		42013, 44351, 46807, 49367, 52081, 54949, 57973, 61169, 64553, 68099,
		71843, 75793, 79967, 84377, 89017, 93911, 99079, 104527, 110281, 116351,
		122753, 129509, 136649, 144161, 152093, 160481, 169307, 178613, 188437, 198811,
		209743, 221281, 233477, 246317, 259867, 274163, 289241, 305147, 321947, 339649,
		358331, 378041, 398833, 420769, 443917, 468353, 494101, 521281, 549949, 580201,
		612113, 645787, 681311, 718801, 758323, 800053, 844061, 890501, 939469, 991147,
		1045663, 1103171, 1163849, 1227871, 1295447, 1366693, 1441871, 1521193, 1604857, 1693129,
		1786261, 1884503, 1988177, 2097517, 2212877, 2334623, 2463029, 2598503, 2741411, 2892191,
		3051317, 3219121, 3396181, 3582967, 3780037, 3987937, 4207277, 4438691, 4682849, 4940389,
		5212111, 5498783, 5801227, 6120311, 6456929, 6812077, 7186759, 7582049, 7999051, 8439007,
		8903177, 9392849, 9909457, 10454491, 11029507, 11636117, 12276127, 12951307, 13663627, 14415131,
		15207967, 16044419, 16926859, 17857843, 18840023, 19876229, 20969419, 22122743, 23339507, 24623171,
		25977449, 27406237, 28913579, 30503833, 32181551, 33951529, 35818877, 37788913, 39867313, 42060017,
		44373317, 46813847, 49388623, 52105007, 54970787, 57994177, 61183873, 64548989, 68099179, 71844667,
		75796153, 79964921, 84362989, 89002957, 93898127, 99062527, 104510969, 110259077, 116323327, 122721149,
		129470797, 136591691, 144104239, 152029981, 160391629, 169213171, 178519897, 188338519, 198697129, 209625467,
		221154877, 233318399, 246150911, 259689211, 273972121, 289040599, 304937827, 321709429, 339403439, 358070639,
		377764553, 398541599, 420461387, 443586779, 467984051, 493723183, 520877957, 549526279, 579750247, 611636491,
		645276509, 680766733, 718208893, 757710397, 799384483, 843350623, 889734929, 938670367, 990297227, 1044763589,
		1102225627, 1162848023, 1226804767, 1294278991, 1365464329, 1440564871, 1519795943, 1603384729, 1691570897, 1784607301,
		1882760773, 1986312577, 2095559777
	};
	
	/**
	 * Searches a prime number that is equal to or larger than the given number
	 * @param n the number
	 * @return the prime number
	 */
	public final static int next(int n) {
		int i = Arrays.binarySearch(PRIMES, n);
		if (i < 0) {
			i = -i - 1;
		}
		return PRIMES[i];
	}
	
	/**
	 * This method has been used to calculate the array of prime numbers.
	 * It reads in a file created by primegen-0.97 (http://cr.yp.to/primegen.html).
	 * primegen uses the Sieve of Atkin and is hence quite fast.
	 * The file should contain all prime numbers from 0..Integer.MAX_VALUE
	 * @param args the program arguments
	 * @throws Exception if something goes wrong
	 */
	public static void main(String[] args) throws Exception {
		//read in the file 
		System.out.println("Reading prime numbers from file ...");
		BufferedReader br = new BufferedReader(new InputStreamReader(
				Primes.class.getResourceAsStream("primes.txt")));
		String line;
		long i = 5;
		List<Integer> primes = new ArrayList<Integer>();
		while ((line = br.readLine()) != null) {
			if (line.isEmpty()) {
				continue;
			}
			long p = Long.parseLong(line);
			long ni = Math.round(i * 1.11);
			if (p >= ni) {
				primes.add((int)p);
				i += (p - i) / 2;
			}
		}
		
		//print out suitable for Java
		System.out.println("private static final int[] PRIMES = {");
		System.out.print("\t");
		int n = 0;
		for (Integer p : primes) {
			if (n > 0) {
				System.out.print(", ");
			}
			System.out.print(p);
			++n;
			if (n == 10) {
				System.out.println(",");
				System.out.print("\t");
				n = 0;
			}
		}
		if (n != 10) {
			System.out.println();
		}
		System.out.println("};");
	}
}
